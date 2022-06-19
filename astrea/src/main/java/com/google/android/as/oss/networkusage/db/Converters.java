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

import androidx.room.TypeConverter;
import arcs.core.data.proto.PolicyProto;
import com.google.android.as.oss.networkusage.api.proto.ConnectionKey;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;

/** Converters between custom classes and Room-identifiable types. */
public class Converters {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  @TypeConverter
  public static ConnectionKey connectionKeyFromBytes(byte[] bytes) {
    try {
      return ConnectionKey.parseFrom(bytes);//, ExtensionRegistryLite.getGeneratedRegistry());
    } catch (InvalidProtocolBufferException e) {
      logger.atSevere().withCause(e).log("Unable to parse ConnectionKey.");
      // Return empty proto message.
      return ConnectionKey.getDefaultInstance();
    }
  }

  @TypeConverter
  public static byte[] connectionKeyToBytes(ConnectionKey connectionKey) {
    return connectionKey.toByteArray();
  }

  @TypeConverter
  public static ConnectionType connectionTypeFromString(String name) {
    return ConnectionType.valueOf(name);
  }

  @TypeConverter
  public static String connectionTypeToString(ConnectionType connectionType) {
    return connectionType.name();
  }

  @TypeConverter
  public static Instant instantFromMillis(long dateMillis) {
    return Instant.ofEpochMilli(dateMillis);
  }

  @TypeConverter
  public static long instantToMillis(Instant instant) {
    return instant.toEpochMilli();
  }

  @TypeConverter
  public static PolicyProto policyProtoFromBytes(byte[] bytes) {
    try {
      return PolicyProto.parseFrom(bytes);//, ExtensionRegistryLite.getGeneratedRegistry());
    } catch (InvalidProtocolBufferException e) {
      logger.atSevere().withCause(e).log("Unable to parse PolicyProto.");
      return PolicyProto.getDefaultInstance();
    }
  }

  @TypeConverter
  public static byte[] policyProtoToBytes(PolicyProto policyProto) {
    return policyProto.toByteArray();
  }

  private Converters() {}
}
