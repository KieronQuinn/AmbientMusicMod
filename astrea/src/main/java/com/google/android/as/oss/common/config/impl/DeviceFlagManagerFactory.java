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

import android.content.Context;
import com.google.android.as.oss.common.config.FlagManager;
import com.google.android.as.oss.common.config.FlagManagerFactory;
import com.google.android.as.oss.common.config.FlagNamespace;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * An implementation of {@link FlagManagerFactory} that returns instances of the {@link
 * DeviceFlagManager}.
 */
class DeviceFlagManagerFactory implements FlagManagerFactory {

  private final Context context;

  @Inject
  DeviceFlagManagerFactory(@ApplicationContext Context context) {
    this.context = context;
  }

  @Override
  public FlagManager create(FlagNamespace namespace, Executor executor) {
    return DeviceFlagManager.create(namespace, executor, context);
  }
}
