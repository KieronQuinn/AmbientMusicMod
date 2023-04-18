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

package com.google.android.as.oss.common.initializer;

/** Defines a task that runs on app start-up. */
public interface PcsInitializer {

  int PRIORITY_HIGH = 10;
  int PRIORITY_DEFAULT = 0;
  int PRIORITY_LOW = -10;

  /** The method to run on app start-up. */
  void run();

  /** Returns the priority of the initializer being run. */
  default int getPriority() {
    return PRIORITY_DEFAULT;
  }
}
