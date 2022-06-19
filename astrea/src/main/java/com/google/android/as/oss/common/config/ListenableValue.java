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

import androidx.annotation.GuardedBy;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Container that emits an observable signal when the value it stores changes. */
// Note that T extends Object explicitly to make the null checker not accept @Nullable.
public class ListenableValue<T extends Object> {
  private final CopyOnWriteArrayList<ValueListener<T>> listeners;

  @GuardedBy("this")
  private @Nullable T value;

  /**
   * Creates a new value with no initial value (null). This state is only available at the beginning
   * of the lifetime of the object; as soon as its value is set, it cannot go back to null (and no
   * listener will be called for the null -> first value transition). Hence, do not use null in
   * general, this factory method is designed to help delayed initialization only.
   */
  public static <T extends Object> ListenableValue<T> create() {
    return new ListenableValue<T>(null);
  }

  public static <T extends Object> ListenableValue<T> create(T initialValue) {
    return new ListenableValue<T>(initialValue);
  }

  public void addListener(ValueListener<T> listener) {
    listeners.add(listener);
  }

  public void removeListener(ValueListener<T> listener) {
    listeners.remove(listener);
  }

  /** Returns the stored value. */
  public synchronized @Nullable T get() {
    return value;
  }

  /**
   * Stores the given value if there was no value already (null). No listener is called.
   *
   * @return stored value after the operation (either the original non-null value or the passed
   *     default).
   */
  public synchronized T putIfAbsent(T defaultValue) {
    if (value == null) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * Stores the value output from the given function if there was no value already (null). No
   * listener is called.
   *
   * @return stored value after the operation (either the original non-null value or the passed
   *     default).
   */
  public synchronized T putIfAbsent(ValueComputer<T> defaultComputer) {
    if (value == null) {
      value = defaultComputer.compute();
    }

    return value;
  }

  /**
   * Refreshes the cached value and notifies listener if it changed.
   *
   * @return whether the value changed and the listeners were called. Note that if the object hasn't
   *     been completely initialized yet (no initial value was provided and there has been no
   *     previous call to either {@link putIfAbsent} or {@link refresh}), the listeners will not be
   *     called.
   */
  public boolean refresh(T newValue) {
    T previous;
    synchronized (this) {
      previous = value;
      value = newValue;
    }

    if (previous == null || newValue.equals(previous)) {
      return false;
    }

    for (ValueListener<T> listener : listeners) {
      listener.onUpdate(newValue, previous);
    }

    return true;
  }

  private ListenableValue(@Nullable T initialValue) {
    this.listeners = new CopyOnWriteArrayList<>();
    this.value = initialValue;
  }

  /** A function that computes a value. */
  public interface ValueComputer<T> {
    T compute();
  }
}
