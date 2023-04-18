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

/** An abstract base implementation of a {@link ConfigReader} providing some common facilities. */
// Note that ConfigT extends Object explicitly to make the null checker not accept @Nullable.
// See [redacted]
@SuppressWarnings("ExtendsObject")
public abstract class AbstractConfigReader<ConfigT extends Object>
    implements ConfigReader<ConfigT> {
  private final ListenableValue<ConfigT> config = ListenableValue.<ConfigT>create();

  @Override
  public void addListener(ValueListener<ConfigT> listener) {
    config.addListener(listener);
  }

  @Override
  public void removeListener(ValueListener<ConfigT> listener) {
    config.removeListener(listener);
  }

  /**
   * Get a reference to the cached configuration.
   *
   * <p>This will fetch the config the first time you use it, so consider calling it once on your
   * factory functions. That way you ensure it's O(1) time in real use. On the other hand, also
   * consider whether your computation function accesses heavyweight resources such as databases or
   * disk files, in which case you might want to move such initialization to a more suitable time
   * and thread.
   */
  @Override
  public synchronized ConfigT getConfig() {
    return config.putIfAbsent(() -> computeConfig());
  }

  /** Refreshes the cached config and notifies listener if it changed. */
  protected void refreshConfig() {
    config.refresh(computeConfig());
  }

  protected abstract ConfigT computeConfig();

  protected AbstractConfigReader() {}
}
