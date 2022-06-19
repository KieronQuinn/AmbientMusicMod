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

package com.google.android.as.oss.common.config;

/**
 * Configuration reader. Merges information from both the settings page for a feature and the
 * experiment flags from the server. Updates its state when settings or flags are changed and
 * notifies interested listeners.
 */
public interface ConfigReader<ConfigT> {
  /** Returns the current configuration values. */
  ConfigT getConfig();

  /** Adds a listener to be notified when config are updated. */
  void addListener(ValueListener<ConfigT> listener);

  /** Removes a listener previous registered via addListener. */
  void removeListener(ValueListener<ConfigT> listener);
}
