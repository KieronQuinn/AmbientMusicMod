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

/**
 * Implemented by classes that can be listened to.
 *
 * <p>There is no explicit contract on which thread the implementor should call the listener
 * callbacks.
 *
 * @param <L> type of the listener.
 */
public interface Listenable<L> {
  /**
   * Adds a listener to the listener list. Note this does not check that the listener already does
   * not already exist in the list so calling it twice with the same listener will add two
   * references of it, and it will get notified twice.
   *
   * @return true if the listener was successfully added.
   */
  boolean addListener(L listener);

  /**
   * Removes a listener from the listener list, if exists in the list.
   *
   * @return true if the listener was removed, false if it wasn't, because it was not in the list.
   */
  boolean removeListener(L listener);
}
