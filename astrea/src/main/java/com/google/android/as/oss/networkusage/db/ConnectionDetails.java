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

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.as.oss.networkusage.api.proto.ConnectionKey;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.base.Preconditions;
import com.ryanharter.auto.value.parcel.ParcelAdapter;
import com.ryanharter.auto.value.parcel.TypeAdapter;

/** Details that describe an PCS connection. */
@AutoValue
public abstract class ConnectionDetails implements Parcelable {
  @CopyAnnotations
  @ParcelAdapter(ConnectionKeyTypeAdapter.class)
  public abstract ConnectionKey connectionKey();

  public abstract ConnectionType type();

  public abstract String packageName();

  public static Builder builder() {
    return new AutoValue_ConnectionDetails.Builder();
  }

  public abstract Builder toBuilder();

  /** Required by Room. Please use {@link Builder} instead. */
  public static ConnectionDetails create(
      ConnectionKey connectionKey, ConnectionType type, String packageName) {
    return builder()
        .setConnectionKey(connectionKey)
        .setType(type)
        .setPackageName(packageName)
        .build();
  }

  /** Builder for creating new instances. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setConnectionKey(ConnectionKey connectionKey);

    public abstract Builder setType(ConnectionType type);

    public abstract Builder setPackageName(String packageName);

    public abstract ConnectionDetails build();
  }

  /** The type of download or upload event. */
  public enum ConnectionType {
    UNKNOWN_TYPE,
    /**
     * A lightweight download to check the eligibility of clients for Federated Computation jobs.
     */
    FC_CHECK_IN,
    /**
     * Represents the start of a Federated Computation query. Each {@code FC_TRAINING_START_QUERY}
     * entity in the database, will have one corresponding {@code FC_TRAINING_RESULT_UPLOAD} entity
     * with the same {@link NetworkUsageEntity#fcRunId}, or zero in case of a failed query.
     */
    FC_TRAINING_START_QUERY,
    /**
     * Represents the upload of a completed Federated Computation job; which consists of one or more
     * queries. Each {@code FC_TRAINING_RESULT_UPLOAD} entity in the database corresponds to one or
     * more {@code FC_TRAINING_START_QUERY} entities with the same {@link
     * NetworkUsageEntity#fcRunId}.
     */
    FC_TRAINING_RESULT_UPLOAD,
    /** A download using HTTPS. */
    HTTP,
    /** A download using Private Information Retrieval. */
    PIR
  }

  /**
   * TypeAdapter that converts ConnectionKey to a parcelable format. Used to implement the Auto
   * Value: Parcelable extension.
   */
  public static class ConnectionKeyTypeAdapter implements TypeAdapter<ConnectionKey> {
    @Override
    public ConnectionKey fromParcel(Parcel in) {
      return Converters.connectionKeyFromBytes(Preconditions.checkNotNull(in.createByteArray()));
    }

    @Override
    public void toParcel(ConnectionKey value, Parcel dest) {
      dest.writeByteArray(Converters.connectionKeyToBytes(value));
    }
  }
}
// Update the version number if this data schema is updated.
