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

package com.google.android.as.oss.grpc;

import static com.google.common.base.Strings.nullToEmpty;
import static io.grpc.protobuf.lite.ProtoLiteUtils.metadataMarshaller;

import androidx.annotation.VisibleForTesting;
import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.util.Optional;

/** Utility methods to work with the gRPC's {@link Status}. */
public final class GrpcStatusProto {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  /** Standard google prefix for representing type URLs. */
  private static final String TYPE_URL_PREFIX = "type.googleapis.com/";

  /** Standard trailing metadata key including details about {@link Status} of the RPC. */
  @VisibleForTesting
  static final Metadata.Key<Status> STATUS_DETAILS_KEY =
      Metadata.Key.of("grpc-status-details-bin", metadataMarshaller(Status.getDefaultInstance()));

  /**
   * Extracts {@link Status} from the given {@link Throwable}.
   *
   * <p>This is equivalent to {@link io.grpc.protobuf.StatusProto#fromThrowable(Throwable)} but
   * works with Protobuf Lite (while the {@code io.grpc} one does not) and regenerates the proto
   * from {@link io.grpc.Status} when the metadata is missing.
   */
  public static Optional<Status> fromThrowable(Throwable throwable) {
    Metadata metadata = io.grpc.Status.trailersFromThrowable(throwable);
    if (metadata != null && metadata.get(STATUS_DETAILS_KEY) != null) {
      return Optional.of(metadata.get(STATUS_DETAILS_KEY));
    }

    io.grpc.Status status = io.grpc.Status.fromThrowable(throwable);
    if (Code.UNKNOWN.equals(status.getCode())) {
      return Optional.empty();
    }
    String message = status.getDescription();

    return Optional.of(
        Status.newBuilder()
            .setCode(status.getCode().value())
            .setMessage(nullToEmpty(message))
            .build());
  }

  /**
   * Returns a {@link StatusRuntimeException} that wraps the given {@link Status}.
   *
   * <p>This is the same as {@link io.grpc.protobuf.StatusProto#toStatusRuntimeException(Status)}
   * but also works with Protobuf Lite (while the {@code io.grpc} one does not).
   */
  public static StatusRuntimeException toStatusRuntimeException(Status proto) {
    Metadata trailers = new Metadata();
    trailers.put(STATUS_DETAILS_KEY, proto);
    io.grpc.Status status = io.grpc.Status.fromCodeValue(proto.getCode());
    if (!proto.getMessage().isEmpty()) {
      status = status.withDescription(proto.getMessage());
    }
    return new StatusRuntimeException(status, trailers);
  }

  /**
   * Retrieves the value of the given type from {@code Status.details}.
   *
   * @param status the {@link Status} to retrieve the value from
   * @param defaultProto the default instance of {@code T}
   * @return the retrieved proto, or {@link Optional#empty()} on nonexistence or a parse error
   */
  public static <T extends MessageLite> Optional<T> findDetail(Status status, T defaultProto) {
    @SuppressWarnings("unchecked") // Safe by contract of getParserForType().
    Parser<T> messageParser = (Parser<T>) defaultProto.getParserForType();
    String typeUrl = genTypeUrl(defaultProto);
    for (Any detail : status.getDetailsList()) {
      if (detail.getTypeUrl().equals(typeUrl)) {
        try {
          return Optional.of(messageParser.parseFrom(detail.getValue()));
        } catch (InvalidProtocolBufferException e) {
          logger.atSevere().withCause(e).log("Failed to parse %s.", typeUrl);
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Utility method to convert a {@link MessageLite} object to {@link Any} which can then be used in
   * the returned {@link Status} as details.
   */
  public static <T extends MessageLite> Any packIntoAny(T msg) {
    return Any.newBuilder().setTypeUrl(genTypeUrl(msg)).setValue(msg.toByteString()).build();
  }

  /**
   * Utility method to generate a type URL for a given {@link MessageLite} object. In reality this
   * needs to be the fully-qualified name of the proto. But Protobuf Lite does not support
   * descriptors, and hence fully-qualified proto names. To unblock our use of {@link Any} in
   * Android, we use our own convention of representing a type URL. As long as both the packing and
   * unpacking ends use this method to generate the typeUrl - we would be able to send and receive
   * type URLs across a GRPC server/client pair.
   */
  @VisibleForTesting
  static <T extends MessageLite> String genTypeUrl(T object) {
    return TYPE_URL_PREFIX + object.getClass().getCanonicalName();
  }

  private GrpcStatusProto() {}
}
