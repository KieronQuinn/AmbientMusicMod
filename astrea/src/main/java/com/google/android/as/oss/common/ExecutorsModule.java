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

package com.google.android.as.oss.common;

import com.google.android.as.oss.common.ExecutorAnnotations.FlExecutorQualifier;
import com.google.android.as.oss.common.ExecutorAnnotations.GeneralExecutorQualifier;
import com.google.android.as.oss.common.ExecutorAnnotations.IoExecutorQualifier;
import com.google.android.as.oss.common.ExecutorAnnotations.PirExecutorQualifier;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.Executor;
import javax.inject.Singleton;

/** Module to provide {@link Executor} instances. */
@Module
@InstallIn(SingletonComponent.class)
public abstract class ExecutorsModule {
  @Provides
  @Singleton
  @FlExecutorQualifier
  static Executor flExecutor() {
    return Executors.FL_EXECUTOR;
  }

  @Provides
  @Singleton
  @GeneralExecutorQualifier
  static Executor generalExecutor() {
    return Executors.GENERAL_SINGLE_THREAD_EXECUTOR;
  }

  @Provides
  @Singleton
  @PirExecutorQualifier
  static Executor pirExecutor() {
    return Executors.PIR_EXECUTOR;
  }

  @Provides
  @Singleton
  @IoExecutorQualifier
  static Executor ioExecutor() {
    return Executors.IO_EXECUTOR;
  }

  private ExecutorsModule() {}
}
