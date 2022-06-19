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

package com.google.android.as.oss.networkusage.db.noop;

import androidx.lifecycle.LiveData;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import com.google.android.as.oss.networkusage.db.NetworkUsageEntity;
import com.google.android.as.oss.networkusage.db.NetworkUsageLogRepository;
import com.google.android.as.oss.networkusage.ui.content.NetworkUsageLogContentMap;
import com.google.common.util.concurrent.FutureCallback;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/** No-op implementation of NetworkUsageLogRepository. */
public class NetworkUsageLogRepositoryNoOpImpl implements NetworkUsageLogRepository {

  @Inject
  NetworkUsageLogRepositoryNoOpImpl() {}

  @Override
  public void insertNetworkUsageEntity(NetworkUsageEntity entity) {}

  @Override
  public boolean isNetworkUsageLogEnabled() {
    return false;
  }

  @Override
  public boolean shouldRejectRequest(ConnectionType type, String connectionKeyString) {
    return false;
  }

  @Override
  public boolean shouldLogNetworkUsage(ConnectionType type, String connectionKeyString) {
    return false;
  }

  @Override
  public boolean isKnownConnection(ConnectionType type, String connectionKeyString) {
    return false;
  }

  @Override
  public Optional<Executor> getDbExecutor() {
    return Optional.empty();
  }

  @Override
  public Optional<NetworkUsageLogContentMap> getContentMap() {
    return Optional.empty();
  }

  @Override
  public LiveData<List<NetworkUsageEntity>> getAll() {
    return new LiveData<List<NetworkUsageEntity>>() {};
  }

  @Override
  public void deleteAllBefore(Instant instant, FutureCallback<Integer> callback) {}
}
