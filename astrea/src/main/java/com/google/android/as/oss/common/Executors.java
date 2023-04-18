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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executor;

/** Declares static Executors used in PCS. */
public class Executors {
  public static final Executor IO_EXECUTOR = java.util.concurrent.Executors.newCachedThreadPool();
  public static final Executor FL_EXECUTOR = java.util.concurrent.Executors.newCachedThreadPool();
  public static final Executor GENERAL_SINGLE_THREAD_EXECUTOR =
      java.util.concurrent.Executors.newSingleThreadExecutor();
  public static final Executor PIR_EXECUTOR = java.util.concurrent.Executors.newCachedThreadPool();

  public static final ListeningScheduledExecutorService PROTECTED_DOWNLOAD_EXECUTOR =
      MoreExecutors.listeningDecorator(
          java.util.concurrent.Executors.newScheduledThreadPool(
              /* corePoolSize= */ 1,
              new ThreadFactoryBuilder().setNameFormat("pcs-pd-%d").build()));

  private Executors() {}
}
