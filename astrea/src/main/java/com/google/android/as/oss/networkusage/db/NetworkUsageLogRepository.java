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

import androidx.lifecycle.LiveData;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import com.google.android.as.oss.networkusage.ui.content.NetworkUsageLogContentMap;
import com.google.common.util.concurrent.FutureCallback;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/** An interface through which the app can interact with the Network Usage Log database. */
public interface NetworkUsageLogRepository {

  /** Adds the given entity into the NetworkUsageLog database. */
  void insertNetworkUsageEntity(NetworkUsageEntity entity);

  /** Returns true if the NetworkUsageLog is enabled. */
  boolean isNetworkUsageLogEnabled();

  /** Returns true if the request with the given type and connectionKeyString should be rejected. */
  boolean shouldRejectRequest(ConnectionType type, String connectionKeyString);

  /** Returns true if the given connection should be logged in the NetworkUsageLog. */
  boolean shouldLogNetworkUsage(ConnectionType type, String connectionKeyString);

  /**
   * Returns true if the given connectionKeyString corresponds to a known connection of the given
   * type.
   */
  boolean isKnownConnection(ConnectionType type, String connectionKeyString);

  /** Returns the executor to run db operations on. */
  Optional<Executor> getDbExecutor();

  /** Returns the NetworkUsageLogContentMap mapping ConnectionDetails to ConnectionResources. */
  Optional<NetworkUsageLogContentMap> getContentMap();

  /** Returns the list of NetworkUsageEntities in the database. */
  LiveData<List<NetworkUsageEntity>> getAll();

  /** Deletes all entities before the given instant. */
  void deleteAllBefore(Instant instant, FutureCallback<Integer> callback);
}
