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

package com.google.android.as.oss.common.config.impl;

import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines an interface to the {@link android.provider.DeviceConfig} object, in order to make it
 * mockable for testing.
 */
public interface DeviceConfigProxy {
  /**
   * Returns a flag value given its name and namespace.
   *
   * <p>See {@link android.provider.DeviceConfig#getProperty(String, String)}.
   */
  @Nullable String getProperty(String namespace, String name);

  /**
   * Adds a listener for the given namespace.
   *
   * <p>See {@link android.provider.DeviceConfig#addOnPropertiesChangedListener}.
   */
  void addOnPropertiesChangedListener(
      String namespace,
      Executor listenerExecutor,
      android.provider.DeviceConfig.OnPropertiesChangedListener listener);
}
