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

package com.google.android.as.oss.common.base;

import static com.google.common.base.Preconditions.checkState;

import com.google.android.as.oss.common.futures.MoreFutures;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

/**
 * Provides a common pattern for safe initialization of components which have the following
 * requirements:
 *
 * <ul>
 *   <li>Asynchronous initialization returning a {@link ListenableFuture} result.
 *   <li>Only one initialization can be run concurrently.
 *   <li>Once initialized successfully, initialization will not be triggered again unless it is
 *       explicitly reset.
 * </ul>
 *
 * @param <R> the expected result type from the initialization of the enclosing class.
 */
public class SafeInitializer<R> {

  private final String initializingClassName;
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final ListenableFuture<R> uninitializedDefaultFuture;
  private final Executor setInitializedExecutor;

  @Nullable private volatile ListenableFuture<R> actualInitFuture;
  private volatile ListenableFuture<R> initFuture;

  /**
   * Creates a new safe initializer for the provided class.
   *
   * @param clazz the class the initializer handles the initialization for.
   * @param <T> the type of the class that is being handled.
   */
  public static <T, R> SafeInitializer<R> forClass(Class<T> clazz) {
    return new SafeInitializer<>(clazz.getSimpleName(), MoreExecutors.directExecutor());
  }

  @VisibleForTesting
  SafeInitializer(String initializingClassName, Executor setInitializedExecutor) {
    this.initializingClassName = initializingClassName;
    this.setInitializedExecutor = setInitializedExecutor;
    uninitializedDefaultFuture =
        Futures.immediateFailedFuture(
            new IllegalStateException(initializingClassName + " is not initialized"));
    initFuture = uninitializedDefaultFuture;
  }

  /**
   * Runs an initialization code provided in {@code actualInit} safely only if there is no running
   * initialization at the moment and returns a {@link ListenableFuture} to indicate when it is
   * done.
   *
   * <p>If an initialization is currently running, it returns the same {@link ListenableFuture} as
   * the running initialization, ignoring the provided {@code actualInit} initialization logic.
   *
   * @return a {@link ListenableFuture} indicating when the initialization is done.
   */
  public synchronized ListenableFuture<R> initialize(Supplier<ListenableFuture<R>> actualInit) {
    if (initNeeded()) {
      ListenableFuture<R> localActualInitFuture = actualInit.get();
      actualInitFuture = localActualInitFuture;
      // Sets initialized = true upon success with direct executor since it is a lightweight
      // operation that will not have a performance impact on the executor service running the
      // actual initialization.
      initFuture =
          Futures.transform(
              actualInitFuture,
              actualReturnValue -> {
                setInitialized(localActualInitFuture);
                return actualReturnValue;
              },
              setInitializedExecutor);
    }
    return initFuture;
  }

  private boolean initNeeded() {
    return initFuture.isDone() && !MoreFutures.isSuccessfulFuture(initFuture);
  }

  /** Returns whether or not the initialization has finished successfully. */
  public boolean isInitialized() {
    return initialized.get();
  }

  /**
   * Verifies that the initialization has finished successfully.
   *
   * @throws IllegalStateException if the initialization has not finished or was not successful.
   */
  public void checkInitialized() {
    checkState(isInitialized(), "%s is not initialized", initializingClassName);
  }

  /**
   * Resets the initialization, forcing the next call to {@link #initialize(Supplier)} to run the
   * provided initialization code.
   */
  public synchronized void reset() {
    initialized.set(false);
    initFuture = uninitializedDefaultFuture;
    actualInitFuture = null;
  }

  private synchronized void setInitialized(ListenableFuture<R> expectedActualInitFuture) {
    if (expectedActualInitFuture == actualInitFuture) {
      initialized.set(true);
    }
  }
}
