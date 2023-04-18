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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import javax.inject.Qualifier;

/** Declares qualifiers for {@link Executor} used in PCS. */
public abstract class ExecutorAnnotations {

  /** Annotation to bind {@link Executor} used for federated jobs. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FlExecutorQualifier {}

  /** Annotation to bind {@link Executor} used in general jobs. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface GeneralExecutorQualifier {}

  /** Annotation to bind {@link Executor} used for PIR jobs. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface PirExecutorQualifier {}

  /** Annotation to bind {@link Executor} used for IO jobs. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface IoExecutorQualifier {}

  /** Annotation to bind {@link Executor} used for protected download. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ProtectedDownloadExecutorQualifier {}

  private ExecutorAnnotations() {}
}
