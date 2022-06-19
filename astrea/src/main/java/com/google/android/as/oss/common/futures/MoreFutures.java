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

package com.google.android.as.oss.common.futures;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * Common utility functions for using {@link Future}s, in addition to those
 * provided in {@link Futures}.
 */
public final class MoreFutures {

  private MoreFutures() {}

  /**
   * Checks if the provided future exists (not <code>null</code>), has finished successfully (i.e.
   * no exception was thrown during the execution of the future) and was not cancelled.
   */
  public static boolean isSuccessfulFuture(@Nullable Future<?> future) {
    if (future == null || !future.isDone()) {
      return false;
    }

    try {
      Futures.getDone(future);
      return true;
    } catch (ExecutionException | CancellationException e) {
      return false;
    }
  }
}
