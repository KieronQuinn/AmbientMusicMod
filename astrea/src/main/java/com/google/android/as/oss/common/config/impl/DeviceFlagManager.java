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

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Context;
import android.os.Binder;
import android.provider.DeviceConfig;
import androidx.annotation.VisibleForTesting;
import com.google.android.as.oss.common.config.AbstractFlagManager;
import com.google.android.as.oss.common.config.FlagListener;
import com.google.android.as.oss.common.config.FlagNamespace;
import com.google.android.as.oss.common.config.Listenable;
import com.google.android.as.oss.common.config.MulticastListenable;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.GoogleLogger;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Instance that wraps {@link DeviceConfig}, which manages Android system-configurable flags. */
// TODO: make this package-private.
public class DeviceFlagManager extends AbstractFlagManager {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private final String namespace;
  private final DeviceConfigProxy deviceConfig;
  private final MulticastListenable<FlagListener> listenable;

  private static final Object LOCK = new Object();
  private static final PoisonedState poisonedState = new PoisonedState();

  // TODO: make this package-private.
  public static DeviceFlagManager create(
      FlagNamespace namespace, Executor executor, Context context) {
    Binder.clearCallingIdentity();
    return new DeviceFlagManager(
        namespace,
        executor,
        new DeviceConfigProxy() {
          @Override
          public @Nullable String getProperty(String namespace, String name) {
            if (!deviceConfigAvailable()) {
              return null;
            }

            return DeviceConfig.getProperty(namespace, name);
          }

          @Override
          public void addOnPropertiesChangedListener(
              String namespace,
              Executor listenerExecutor,
              DeviceConfig.OnPropertiesChangedListener listener) {
            if (!deviceConfigAvailable()) {
              return;
            }

            DeviceConfig.addOnPropertiesChangedListener(namespace, listenerExecutor, listener);
          }

          private boolean deviceConfigAvailable() {
            // No need to lock -> can only change from empty to present.
            if (poisonedState.getIsPoisoned().isPresent()) {
              return poisonedState.getIsPoisoned().get();
            }

            synchronized (LOCK) {
              if (poisonedState.getIsPoisoned().isEmpty()) {
                poisonedState.markPoisoned(
                    context.checkSelfPermission("android.permission.READ_DEVICE_CONFIG")
                        == PERMISSION_GRANTED);
              }
            }

            if (!poisonedState.getIsPoisoned().get()) {
              logger.atSevere().log(
                  "Device Config not available for com.google.android.as: relying on defaults");
            }
            return poisonedState.getIsPoisoned().get();
          }
        });
  }

  /**
   * Creates an instance of a {@link DeviceFlagManager} with the given {@link DeviceConfigProxy}
   * object. This factory variant is used for testing to be able to mock the system's {@link
   * DeviceConfig} object.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public static DeviceFlagManager create(
      FlagNamespace namespace, Executor executor, DeviceConfigProxy deviceConfig) {
    return new DeviceFlagManager(namespace, executor, deviceConfig);
  }

  // This has to be a proper singleton implementation with well thought-over API.
  private static class PoisonedState {
    private Optional<Boolean> isPoisoned = Optional.empty();

    void markPoisoned(boolean poisoned) {
      isPoisoned = Optional.of(poisoned);
    }

    private Optional<Boolean> getIsPoisoned() {
      return isPoisoned;
    }
  }

  @Override
  public @Nullable String getProperty(String name) {
    return deviceConfig.getProperty(namespace, name);
  }

  @Override
  public Listenable<FlagListener> listenable() {
    return listenable;
  }

  @SuppressWarnings("argument.type.incompatible")
  private DeviceFlagManager(
      FlagNamespace namespace, Executor listenerExecutor, DeviceConfigProxy deviceConfig) {
    this.namespace = namespace.toString();
    this.deviceConfig = deviceConfig;

    // The default MulticastListenable uses a direct executor to avoid thread context switches since
    // we are already relying on the device config code to call our callback in the given listener
    // executor.
    listenable = MulticastListenable.create();
    deviceConfig.addOnPropertiesChangedListener(
        namespace.toString(),
        listenerExecutor,
        properties -> {
          ImmutableList<String> changedFlags = ImmutableList.copyOf(properties.getKeyset());
          listenable.notify(listener -> listener.onUpdated(changedFlags));
        });
  }
}
