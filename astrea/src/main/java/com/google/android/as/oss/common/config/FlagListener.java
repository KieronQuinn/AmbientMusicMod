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

import java.util.Collection;
import java.util.List;

/** Listener of changes to flags. */
@SuppressWarnings("AndroidApiChecker") // TODO
public interface FlagListener {
  /**
   * Notifies that a set of flags have been updated.
   *
   * @param flagNames the names of the flags that have been updated.
   */
  void onUpdated(List<String> flagNames);

  /** Returns {@code true} if the set of flags contains at least one flag with the target prefix. */
  static boolean anyHasPrefix(Collection<String> flagNames, String targetPrefix) {
    return flagNames.stream().anyMatch(flagName -> flagName.startsWith(targetPrefix));
  }
}
