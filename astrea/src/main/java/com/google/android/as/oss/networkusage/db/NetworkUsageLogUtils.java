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

package com.google.android.as.oss.networkusage.db;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import arcs.core.data.proto.PolicyProto;
import com.google.android.as.oss.networkusage.api.proto.ConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.FlConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.HttpConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.PirConnectionKey;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import com.google.common.base.Strings;

/** Utility class for creating NetworkUsageLog entities. */
public final class NetworkUsageLogUtils {

  public static ConnectionDetails createHttpConnectionDetails(String urlRegex, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(urlRegex));
    return getDefaultConnectionDetailsBuilder(ConnectionType.HTTP, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setHttpConnectionKey(HttpConnectionKey.newBuilder().setUrlRegex(urlRegex).build())
                .build())
        .build();
  }

  public static ConnectionDetails createPirConnectionDetails(String urlRegex, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(urlRegex));
    return getDefaultConnectionDetailsBuilder(ConnectionType.PIR, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setPirConnectionKey(PirConnectionKey.newBuilder().setUrlRegex(urlRegex).build())
                .build())
        .build();
  }

  public static ConnectionDetails createPdConnectionDetails(String clientId, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(clientId));
    return getDefaultConnectionDetailsBuilder(ConnectionType.PD, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setPdConnectionKey(
                    com.google.android.as.oss.networkusage.api.proto.PdConnectionKey.newBuilder()
                        .setClientId(clientId)
                        .build())
                .build())
        .build();
  }

  public static ConnectionDetails createFcCheckInConnectionDetails() {
    return getDefaultConnectionDetailsBuilder(ConnectionType.FC_CHECK_IN).build();
  }

  public static ConnectionDetails createFcTrainingResultUploadConnectionDetails() {
    return getDefaultConnectionDetailsBuilder(ConnectionType.FC_TRAINING_RESULT_UPLOAD).build();
  }

  public static ConnectionDetails createFcTrainingStartQueryConnectionDetails(
      String featureName, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(featureName));
    return getDefaultConnectionDetailsBuilder(ConnectionType.FC_TRAINING_START_QUERY, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setFlConnectionKey(
                    FlConnectionKey.newBuilder().setFeatureName(featureName).build())
                .build())
        .build();
  }

  public static NetworkUsageEntity createHttpNetworkUsageEntity(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(connectionDetails.connectionKey().hasHttpConnectionKey());
    checkArgument(
        url.matches(connectionDetails.connectionKey().getHttpConnectionKey().getUrlRegex()));
    checkArgument(connectionDetails.type() == ConnectionType.HTTP);
    return getNetworkUsageEntityForUrl(connectionDetails, status, downloadSize, url);
  }

  public static NetworkUsageEntity createPirNetworkUsageEntity(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(connectionDetails.type() == ConnectionType.PIR);
    checkArgument(connectionDetails.connectionKey().hasPirConnectionKey());
    checkArgument(
        url.matches(connectionDetails.connectionKey().getPirConnectionKey().getUrlRegex()));
    return getNetworkUsageEntityForUrl(connectionDetails, status, downloadSize, url);
  }

  public static NetworkUsageEntity createFcCheckInNetworkUsageEntity(
      long downloadSize, long uploadSize) {
    return NetworkUsageEntity.defaultBuilder()
        .setConnectionDetails(createFcCheckInConnectionDetails())
        .setStatus(Status.SUCCEEDED)
        .setDownloadSize(downloadSize)
        .setUploadSize(uploadSize)
        .build();
  }

  public static NetworkUsageEntity createFcTrainingResultNetworkUsageEntity(
      long runId, long downloadSize, long uploadSize) {
    return NetworkUsageEntity.defaultBuilder()
        .setConnectionDetails(createFcTrainingResultUploadConnectionDetails())
        .setStatus(Status.SUCCEEDED)
        .setFcRunId(runId)
        .setDownloadSize(downloadSize)
        .setUploadSize(uploadSize)
        .build();
  }

  public static NetworkUsageEntity createFcTrainingStartQueryNetworkUsageEntity(
      ConnectionDetails connectionDetails, long runId, PolicyProto policyProto) {
    checkArgument(connectionDetails.type() == ConnectionType.FC_TRAINING_START_QUERY);
    checkArgument(connectionDetails.connectionKey().hasFlConnectionKey());
    checkNotNull(policyProto);
    checkArgument(policyProto.isInitialized());
    return getNetworkUsageEntityBuilder(connectionDetails, Status.SUCCEEDED, /* downloadSize= */ 0)
        .setFcRunId(runId)
        .setPolicyProto(policyProto)
        .build();
  }

  public static NetworkUsageEntity createPdNetworkUsageEntry(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String clientId) {
    checkArgument(connectionDetails.type() == ConnectionType.PD);
    checkArgument(connectionDetails.connectionKey().hasPdConnectionKey());
    checkArgument(
        clientId.equals(connectionDetails.connectionKey().getPdConnectionKey().getClientId()));
    return getNetworkUsageEntityBuilder(connectionDetails, status, downloadSize).build();
  }

  private static NetworkUsageEntity getNetworkUsageEntityForUrl(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return getNetworkUsageEntityBuilder(connectionDetails, status, downloadSize)
        .setUrl(url)
        .build();
  }

  private static NetworkUsageEntity.Builder getNetworkUsageEntityBuilder(
      ConnectionDetails connectionDetails, Status status, long downloadSize) {
    checkNotNull(connectionDetails);
    checkNotNull(status);
    checkArgument(downloadSize >= 0);

    return NetworkUsageEntity.defaultBuilder()
        .setConnectionDetails(connectionDetails)
        .setStatus(status)
        .setDownloadSize(downloadSize);
  }

  private static ConnectionDetails.Builder getDefaultConnectionDetailsBuilder(
      ConnectionType type, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(packageName));
    return getDefaultConnectionDetailsBuilder(type).setPackageName(packageName);
  }

  private static ConnectionDetails.Builder getDefaultConnectionDetailsBuilder(ConnectionType type) {
    return ConnectionDetails.builder()
        .setType(type)
        .setPackageName("unknown")
        .setConnectionKey(ConnectionKey.getDefaultInstance());
  }

  private NetworkUsageLogUtils() {}
}
