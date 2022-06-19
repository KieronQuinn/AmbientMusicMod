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

import com.google.errorprone.annotations.Immutable;

/**
 * Represents an operation that accepts a single input argument and returns no result. {@code
 * Consumer} is expected to operate via side-effects.
 *
 * <p>This class mimics the Java 8 Consumer, without the Java 8 specific features for API
 * independent Android support.
 *
 * @param <T> the type of the input to the operation
 */
@Immutable
public interface Consumer<T> {

  /**
   * Performs this operation on the given argument.
   *
   * @param t the input argument
   */
  void accept(T t);
}
