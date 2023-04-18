/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.as.oss.http.service;

import static com.google.android.as.oss.grpc.ContextKeys.WRITEABLE_FILE_CONTEXT_KEY;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.as.oss.common.ExecutorAnnotations.IoExecutorQualifier;
import com.google.android.as.oss.common.config.ConfigReader;
import com.google.android.as.oss.grpc.GrpcStatusProto;
import com.google.android.as.oss.http.api.proto.HttpDownloadRequest;
import com.google.android.as.oss.http.api.proto.HttpDownloadResponse;
import com.google.android.as.oss.http.api.proto.HttpProperty;
import com.google.android.as.oss.http.api.proto.HttpServiceGrpc;
import com.google.android.as.oss.http.api.proto.ResponseBodyChunk;
import com.google.android.as.oss.http.api.proto.ResponseHeaders;
import com.google.android.as.oss.http.api.proto.UnrecognizedUrlException;
import com.google.android.as.oss.http.config.PcsHttpConfig;
import com.google.android.as.oss.networkusage.db.ConnectionDetails;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import com.google.android.as.oss.networkusage.db.NetworkUsageEntity;
import com.google.android.as.oss.networkusage.db.NetworkUsageLogRepository;
import com.google.android.as.oss.networkusage.db.NetworkUsageLogUtils;
import com.google.android.as.oss.networkusage.db.Status;
import com.google.android.as.oss.networkusage.ui.content.UnrecognizedNetworkRequestException;
import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Bindable Service that handles HTTP requests to Private Compute Services. */
public class HttpGrpcBindableService extends HttpServiceGrpc.HttpServiceImplBase {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  @VisibleForTesting static final int BUFFER_LENGTH = 1_024;

  private final OkHttpClient client;
  private final Executor executor;
  private final NetworkUsageLogRepository networkUsageLogRepository;
  private final ConfigReader<PcsHttpConfig> configReader;

  @Inject
  HttpGrpcBindableService(
      OkHttpClient client,
      @IoExecutorQualifier Executor ioExecutor,
      NetworkUsageLogRepository networkUsageLogRepository,
      ConfigReader<PcsHttpConfig> httpConfigReader) {
    this.client = client;
    this.executor = ioExecutor;
    this.networkUsageLogRepository = networkUsageLogRepository;
    this.configReader = httpConfigReader;
  }

  @Override
  public void download(
      HttpDownloadRequest request, StreamObserver<HttpDownloadResponse> responseObserver) {
    logger.atInfo().log("Downloading requested for URL '%s'", request.getUrl());
    // Snapshot config so we use consistent values for a single request.
    PcsHttpConfig config = configReader.getConfig();

    // To suppress an exception in onNext in case the observer was cancelled.
    ((ServerCallStreamObserver<HttpDownloadResponse>) responseObserver)
        .setOnCancelHandler(() -> {});

    // Reject plain HTTP URLs from downloader.
    if (!isValidHttpsUrl(request.getUrl())) {
      logger.atWarning().log("Rejected non HTTPS url request to PCS");
      responseObserver.onError(
          new IllegalArgumentException(
              String.format("Rejecting non HTTPS url: '%s'", request.getUrl())));
      return;
    }

    // Log Unrecognized requests
    if (!networkUsageLogRepository.isKnownConnection(ConnectionType.HTTP, request.getUrl())) {
      logger.atInfo().log("Network usage log unrecognised HTTPS request for %s", request.getUrl());
    }

    if (networkUsageLogRepository.shouldRejectRequest(ConnectionType.HTTP, request.getUrl())) {
      UnrecognizedNetworkRequestException exception =
          UnrecognizedNetworkRequestException.forUrl(request.getUrl());
      logger.atWarning().withCause(exception).log("Rejected unknown HTTPS request to PCS");

      com.google.rpc.Status statusProto =
          com.google.rpc.Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT.value())
              .setMessage(exception.getMessage())
              .addDetails(
                  GrpcStatusProto.packIntoAny(
                      UnrecognizedUrlException.newBuilder().setUrl(request.getUrl()).build()))
              .build();
      responseObserver.onError(GrpcStatusProto.toStatusRuntimeException(statusProto));
      return;
    }

    Request.Builder okRequest = new Request.Builder().url(request.getUrl());

    for (HttpProperty property : request.getRequestPropertyList()) {
      for (String value : property.getValueList()) {
        okRequest.addHeader(property.getKey(), value);
      }
    }

    Response response;
    try {
      response = client.newCall(okRequest.build()).execute();
    } catch (IOException e) {
      responseObserver.onError(e);
      insertNetworkUsageLogRow(networkUsageLogRepository, request, Status.FAILED, 0L);
      return;
    }

    ResponseHeaders.Builder responseHeaders =
        ResponseHeaders.newBuilder().setResponseCode(response.code());
    for (String name : response.headers().names()) {
      responseHeaders.addHeader(
          HttpProperty.newBuilder()
              .setKey(name)
              .addAllValue(response.headers().values(name))
              .build());
    }

    logger.atInfo().log("Responding with header information for URL '%s'", request.getUrl());
    responseObserver.onNext(
        HttpDownloadResponse.newBuilder().setResponseHeaders(responseHeaders).build());

    ResponseBody body = response.body();

    if (body == null) {
      logger.atInfo().log(
          "Received an empty body for URL '%s'. Responding with fetch_completed.",
          request.getUrl());
      responseObserver.onCompleted();
      insertNetworkUsageLogRow(networkUsageLogRepository, request, Status.SUCCEEDED, 0L);
      return;
    }

    final ParcelFileDescriptor pfd = config.writeToPfd() ? WRITEABLE_FILE_CONTEXT_KEY.get() : null;
    if (config.onReadyHandlerEnabled() && pfd == null) {
      // Only use onReadyHandler if it is enabled by flag AND we have not received a direct pfd.
      ServerCallStreamObserver<HttpDownloadResponse> serverStreamObserver =
          (ServerCallStreamObserver<HttpDownloadResponse>) responseObserver;

      Runnable onReadyHandler =
          new HttpGrpcStreamHandler(
              request,
              serverStreamObserver,
              body.byteStream(),
              config.ipcStreamingThrottleMs(),
              executor,
              networkUsageLogRepository);
      serverStreamObserver.setOnReadyHandler(onReadyHandler);
      // First call is required to be manual as per GRPC docs.
      onReadyHandler.run();
    } else {
      executor.execute(
          new Runnable() {
            private long totalBytesRead = 0;

            @Override
            public void run() {
              try (InputStream is = body.byteStream();
                  OutputStream os =
                      (pfd == null) ? null : new ParcelFileDescriptor.AutoCloseOutputStream(pfd)) {
                byte[] buffer = new byte[BUFFER_LENGTH];

                while (true) {
                  if (((ServerCallStreamObserver<HttpDownloadResponse>) responseObserver)
                      .isCancelled()) {
                    logCallCancelledByClient(
                        null, networkUsageLogRepository, request, totalBytesRead);
                    return;
                  }
                  int bytesRead = is.read(buffer);
                  if (bytesRead == -1) {
                    break;
                  }

                  if (bytesRead > 0) {
                    totalBytesRead += bytesRead;
                    if (os != null) {
                      os.write(buffer, 0, bytesRead);
                    } else {
                      responseObserver.onNext(
                          HttpDownloadResponse.newBuilder()
                              .setResponseBodyChunk(
                                  ResponseBodyChunk.newBuilder()
                                      .setResponseBytes(ByteString.copyFrom(buffer, 0, bytesRead))
                                      .build())
                              .build());
                    }
                  }
                }

                logger.atInfo().log(
                    "[pfd-write] DOWNLOAD COMPLETE: Downloaded %d bytes from URL [%s].",
                    totalBytesRead, request.getUrl());
                responseObserver.onCompleted();
                insertNetworkUsageLogRow(
                    networkUsageLogRepository, request, Status.SUCCEEDED, totalBytesRead);
              } catch (IOException e) {
                logger.atWarning().withCause(e).log(
                    "Failed performing IO operation while handling URL '%s'", request.getUrl());
                responseObserver.onError(e);
                insertNetworkUsageLogRow(
                    networkUsageLogRepository, request, Status.FAILED, totalBytesRead);
              } catch (StatusRuntimeException e) {
                if (((ServerCallStreamObserver) responseObserver).isCancelled()) {
                  logCallCancelledByClient(e, networkUsageLogRepository, request, totalBytesRead);
                } else {
                  responseObserver.onError(e);
                  insertNetworkUsageLogRow(
                      networkUsageLogRepository, request, Status.FAILED, totalBytesRead);
                }
              }
            }
          });
    }
  }

  private boolean isValidHttpsUrl(String url) {
    return url.startsWith("https://");
  }

  private static void logCallCancelledByClient(
      @Nullable Throwable error,
      NetworkUsageLogRepository networkUsageLogRepository,
      HttpDownloadRequest request,
      long size) {
    logger.atWarning().withCause(error).log(
        "Failed to fetch response body for URL '%s'. Call cancelled by client.", request.getUrl());
    insertNetworkUsageLogRow(networkUsageLogRepository, request, Status.FAILED, size);
  }

  private static void insertNetworkUsageLogRow(
      NetworkUsageLogRepository networkUsageLogRepository,
      HttpDownloadRequest request,
      Status status,
      long size) {
    if (!networkUsageLogRepository.shouldLogNetworkUsage(ConnectionType.HTTP, request.getUrl())
        || !networkUsageLogRepository.getContentMap().isPresent()) {
      return;
    }

    ConnectionDetails connectionDetails =
        networkUsageLogRepository
            .getContentMap()
            .get()
            .getHttpConnectionDetails(request.getUrl())
            .get();
    NetworkUsageEntity entity =
        NetworkUsageLogUtils.createHttpNetworkUsageEntity(
            connectionDetails, status, size, request.getUrl());

    networkUsageLogRepository.insertNetworkUsageEntity(entity);
  }

  private static class HttpGrpcStreamHandler implements Runnable {
    private final AtomicLong totalBytesRead = new AtomicLong(0);

    @GuardedBy("this")
    private final byte[] buffer = new byte[BUFFER_LENGTH];

    @GuardedBy("this")
    private int bytesPendingToBeSent = 0;

    private final HttpDownloadRequest request;
    private final ServerCallStreamObserver<HttpDownloadResponse> responseObserver;
    private final InputStream bodyStream;
    private final int ipcStreamingThrottleMs;
    private final Executor backgroundExecutor;
    private final NetworkUsageLogRepository networkUsageLogRepository;

    public HttpGrpcStreamHandler(
        HttpDownloadRequest request,
        ServerCallStreamObserver<HttpDownloadResponse> serverStreamObserver,
        InputStream is,
        int ipcStreamingThrottleMs,
        Executor executor,
        NetworkUsageLogRepository networkUsageLogRepository) {
      this.request = request;
      this.responseObserver = serverStreamObserver;
      this.bodyStream = is;
      this.ipcStreamingThrottleMs = ipcStreamingThrottleMs;
      this.backgroundExecutor = executor;
      this.networkUsageLogRepository = networkUsageLogRepository;
    }

    @Override
    public synchronized void run() {
      logger.atFine().log(
          "onReadyHandler called for URL [%s]. Bytes sent so far: [%d].",
          request.getUrl(), totalBytesRead.get());
      boolean saveStreamToResumeWhenClientIsReadyAgain = false;
      try {
        while (bytesPendingToBeSent >= 0) {
          // Do not overwrite the buffer with new data if previous buffer has not been transmitted.
          if (bytesPendingToBeSent == 0) {
            if (responseObserver.isCancelled()) {
              logCallCancelledByClient(
                  null, networkUsageLogRepository, request, totalBytesRead.get());
              return;
            }
            bytesPendingToBeSent = bodyStream.read(buffer);
          }

          if (bytesPendingToBeSent == -1) {
            // stream is over
            break;
          }

          if (bytesPendingToBeSent > 0) {
            if (!responseObserver.isReady()) {
              // We have received a valid chunk, but client is busy processing previous data. We
              // return for now, but the data is saved in the member buffer to be processed next
              // time.
              saveStreamToResumeWhenClientIsReadyAgain = true;
              return;
            }
            responseObserver.onNext(
                HttpDownloadResponse.newBuilder()
                    .setResponseBodyChunk(
                        ResponseBodyChunk.newBuilder()
                            .setResponseBytes(ByteString.copyFrom(buffer, 0, bytesPendingToBeSent))
                            .build())
                    .build());
            totalBytesRead.addAndGet(bytesPendingToBeSent);
            logger.atFine().log(
                "PCS sent [%d] bytes to client (%d bytes sent so far).",
                bytesPendingToBeSent, totalBytesRead.get());
            // Data has been sent, nothing more to process for now.
            bytesPendingToBeSent = 0;

            // If throttling is enabled (value > 0), sleep for x ms after every 16MB download.
            // Note that totalBytesRead can't be guaranteed to arrive at a particular number, but it
            // increases by BUFFER_LENGTH increments, so %16MB will be less than BUFFER_LENGTH
            // exactly once after every 16MB of download.
            if (ipcStreamingThrottleMs > 0 && totalBytesRead.get() % 16_000_000 < BUFFER_LENGTH) {
              logger.atFine().log("Throttling download from PCS.");
              SystemClock.sleep(ipcStreamingThrottleMs);
            }
          }
        }

        logger.atInfo().log(
            "[onReadyHandler] DOWNLOAD COMPLETE: Downloaded %d bytes from URL [%s].",
            totalBytesRead.get(), request.getUrl());
        try {
          responseObserver.onCompleted();
        }catch (IllegalStateException e){
          //???
        }
        backgroundExecutor.execute(
            () ->
                insertNetworkUsageLogRow(
                    networkUsageLogRepository, request, Status.SUCCEEDED, totalBytesRead.get()));
      } catch (IOException e) {
        logger.atWarning().withCause(e).log(
            "Failed performing IO operation while downloading URL [%s].", request.getUrl());
        responseObserver.onError(e);
        backgroundExecutor.execute(
            () ->
                insertNetworkUsageLogRow(
                    networkUsageLogRepository, request, Status.FAILED, totalBytesRead.get()));
      } catch (StatusRuntimeException e) {
        if (responseObserver.isCancelled()) {
          logCallCancelledByClient(e, networkUsageLogRepository, request, totalBytesRead.get());
        } else {
          responseObserver.onError(e);
          // TODO: Cancellation should also be logged in network usage log.
          backgroundExecutor.execute(
              () ->
                  insertNetworkUsageLogRow(
                      networkUsageLogRepository, request, Status.FAILED, totalBytesRead.get()));
        }
      } finally {
        if (!saveStreamToResumeWhenClientIsReadyAgain) {
          try {
            bodyStream.close();
          } catch (IOException e) {
            logger.atWarning().withCause(e).log(
                "Encountered an error while closing the download stream");
          }
        } else {
          logger.atFine().log("Keeping download stream open for URL: [%s]", request.getUrl());
        }
      }
    }
  }
}
