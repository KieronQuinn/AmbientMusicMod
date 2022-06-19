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

import static java.util.Comparator.comparing;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import arcs.core.data.proto.PolicyProto;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.base.Preconditions;
import com.ryanharter.auto.value.parcel.ParcelAdapter;
import com.ryanharter.auto.value.parcel.TypeAdapter;
import java.time.Instant;
import java.util.Comparator;

/** Room entity representing an entry of PCS's Network Usage Log. */
@AutoValue
@Entity(tableName = "NetworkUsageLog")
public abstract class NetworkUsageEntity implements Parcelable {

  /** Comparator for sorting by decreasing creation time. */
  public static final Comparator<NetworkUsageEntity> BY_LATEST_TIMESTAMP =
      comparing((NetworkUsageEntity arg) -> arg.creationTime(), Comparator.reverseOrder());

  @CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  public abstract int id();

  @CopyAnnotations
  @Embedded
  public abstract ConnectionDetails connectionDetails();

  /** Required for HTTP and PIR downloads only. The download url. */
  public abstract String url();

  /** The success status of the download/upload. */
  public abstract Status status();

  /** The download/upload size in bytes. */
  public abstract long size();

  /** The instantaneous point of creation of the entry. */
  public abstract Instant creationTime();

  /**
   * Required for FC_TRAINING_START_QUERY and FC_TRAINING_RESULT_UPLOAD events only. The id of the
   * FC task. A unique runId corresponds to one or more FC_TRAINING_START_QUERY events, and one or
   * zero FC_TRAINING_RESULT_UPLOAD events.
   */
  @CopyAnnotations
  @ColumnInfo(defaultValue = "-1")
  public abstract long fcRunId();

  /**
   * Required for FC_TRAINING_START_QUERY events only. The policy that the query is compliant with.
   */
  @CopyAnnotations
  @ParcelAdapter(PolicyProtoTypeAdapter.class)
  public abstract PolicyProto policyProto();

  public static Builder defaultBuilder() {
    return builder()
        .setId(0) // Allows room to auto-generate ids
        .setCreationTime(Instant.now())
        .setUrl("")
        .setFcRunId(-1L)
        .setPolicyProto(PolicyProto.getDefaultInstance());
  }

  public abstract Builder toBuilder();

  /** Required by Room. Please use {@link Builder} instead. */
  public static NetworkUsageEntity create(
      String url,
      int id,
      ConnectionDetails connectionDetails,
      Status status,
      long size,
      Instant creationTime,
      long fcRunId,
      PolicyProto policyProto) {
    return builder()
        .setId(id)
        .setConnectionDetails(connectionDetails)
        .setUrl(url)
        .setStatus(status)
        .setSize(size)
        .setCreationTime(creationTime)
        .setFcRunId(fcRunId)
        .setPolicyProto(policyProto)
        .build();
  }

  private static Builder builder() {
    return new AutoValue_NetworkUsageEntity.Builder();
  }

  /** Builder for creating new instances. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(int id);

    public abstract Builder setConnectionDetails(ConnectionDetails connectionDetails);

    public abstract Builder setUrl(String url);

    public abstract Builder setStatus(Status status);

    public abstract Builder setSize(long size);

    public abstract Builder setCreationTime(Instant instant);

    public abstract Builder setFcRunId(long fcRunId);

    public abstract Builder setPolicyProto(PolicyProto policyProto);

    public abstract NetworkUsageEntity build();
  }

  /**
   * TypeAdapter that converts PolicyProto to a parcelable format. Used to implement the Auto Value:
   * Parcelable extension.
   */
  public static class PolicyProtoTypeAdapter implements TypeAdapter<PolicyProto> {
    @Override
    public PolicyProto fromParcel(Parcel in) {
      return Converters.policyProtoFromBytes(Preconditions.checkNotNull(in.createByteArray()));
    }

    @Override
    public void toParcel(PolicyProto value, Parcel dest) {
      dest.writeByteArray(Converters.policyProtoToBytes(value));
    }
  }
}
// Update the version number if this data schema is updated.
