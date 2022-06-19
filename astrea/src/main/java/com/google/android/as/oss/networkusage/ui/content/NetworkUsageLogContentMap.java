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

package com.google.android.as.oss.networkusage.ui.content;

import com.google.android.as.oss.networkusage.db.ConnectionDetails;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import java.util.Optional;

/** Provides a mapping of ConnectionDetails to UI resources for entries in the Network Usage Log. */
public interface NetworkUsageLogContentMap {
  /**
   * Returns the ConnectionDetails object for HTTP connections with the url regex corresponding to
   * the given url.
   */
  Optional<ConnectionDetails> getHttpConnectionDetails(String url);

  /**
   * Returns the ConnectionDetails object for PIR connections with the url regex corresponding to
   * the given url.
   */
  Optional<ConnectionDetails> getPirConnectionDetails(String url);

  /**
   * Returns the ConnectionDetails object for the FC_TRAINING connection with the given feature
   * name.
   */
  Optional<ConnectionDetails> getFcStartQueryConnectionDetails(String featureName);

  /** Returns the feature name corresponding to the given ConnectionDetails. */
  Optional<String> getFeatureName(ConnectionDetails connectionDetails);

  /** Returns the feature description corresponding to the given ConnectionDetails. */
  Optional<String> getDescription(ConnectionDetails connectionDetails);

  /** Returns the name of the download/upload mechanism based on the {@link ConnectionType} */
  String getMechanismName(ConnectionDetails connectionDetails);
}