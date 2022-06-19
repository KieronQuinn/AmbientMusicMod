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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import javax.annotation.concurrent.Immutable;

/** Connector for system-configurable flags. */
public interface FlagManager {
  /**
   * Returns a {@link Listenable} object that callers can use to subscribe to be notified of flag
   * updates.
   */
  Listenable<FlagListener> listenable();

  /**
   * Returns a boolean flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link BooleanFlag}.
   */
  Boolean get(BooleanFlag flag, Boolean defaultOverride);

  /**
   * Returns an integer flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link IntegerFlag}.
   */
  Integer get(IntegerFlag flag, Integer defaultOverride);

  /**
   * Returns a long flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link IntegerFlag}.
   */
  Long get(LongFlag flag, Long defaultOverride);

  /**
   * Returns a float flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link FloatFlag}.
   */
  Float get(FloatFlag flag, Float defaultOverride);

  /**
   * Returns a string flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link StringFlag}.
   */
  String get(StringFlag flag, String defaultOverride);

  /**
   * Returns an enum flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link EnumFlag}.
   */
  <T extends Enum<T>> T get(EnumFlag<T> flag, T defaultOverride);

  /**
   * Returns a string list flag value.
   *
   * <p>If a non-null defaultOverride is provided it will be used instead of the default that is set
   * within {@link StringFlag}.
   */
  ImmutableList<String> get(StringListFlag flag, ImmutableList<String> defaultOverride);

  /** Returns a boolean flag value. */
  default Boolean get(BooleanFlag flag) {
    return get(flag, flag.defaultValue());
  }

  /** Returns an integer flag value. */
  default Integer get(IntegerFlag flag) {
    return get(flag, flag.defaultValue());
  }

  /** Returns a long flag value. */
  default Long get(LongFlag flag) {
    return get(flag, flag.defaultValue());
  }

  /** Returns a float flag value. */
  default Float get(FloatFlag flag) {
    return get(flag, flag.defaultValue());
  }

  /** Returns a string flag value. */
  default String get(StringFlag flag) {
    return get(flag, flag.defaultValue());
  }

  /** Returns an enum flag value. */
  default <T extends Enum<T>> T get(EnumFlag<T> flag) {
    return get(flag, flag.defaultValue());
  }

  /** Returns a string list flag value. */
  default ImmutableList<String> get(StringListFlag flag) {
    return get(flag, flag.defaultValue());
  }

  /**
   * Encapsulates a device config flag configured by the system
   *
   * <p>Flags use Android platform flags and can't use code-generated wrappers. This type wraps
   * values that a flag specification needs to avoid duplication.
   *
   * @param <T> The given type for the flag.
   */
  @Immutable
  abstract class Flag<T> {
    public abstract String name();

    public abstract T defaultValue();
  }

  /** {@link Flag} implementation of {@link Boolean} type. */
  @AutoValue
  abstract class BooleanFlag extends Flag<Boolean> {
    /** Creates a flag containing a {@link Boolean} value. */
    public static BooleanFlag create(String name, Boolean defaultValue) {
      return new AutoValue_FlagManager_BooleanFlag(name, defaultValue);
    }
  }

  /** {@link Flag} implementation of {@link Integer} type. */
  @AutoValue
  abstract class IntegerFlag extends Flag<Integer> {
    /** Creates a flag containing a {@link Integer} value. */
    public static IntegerFlag create(String name, Integer defaultValue) {
      return new AutoValue_FlagManager_IntegerFlag(name, defaultValue);
    }
  }

  /** {@link Flag} implementation of {@link Long} type. */
  @AutoValue
  abstract class LongFlag extends Flag<Long> {
    /** Creates a flag containing a {@link Long} value. */
    public static LongFlag create(String name, Long defaultValue) {
      return new AutoValue_FlagManager_LongFlag(name, defaultValue);
    }
  }

  /** {@link Flag} implementation of {@link String} type. */
  @AutoValue
  abstract class StringFlag extends Flag<String> {
    /** Creates a flag containing a {@link String} value. */
    public static StringFlag create(String name, String defaultValue) {
      return new AutoValue_FlagManager_StringFlag(name, defaultValue);
    }
  }

  /** {@link Flag} implementation of {@link Float} type. */
  @AutoValue
  abstract class FloatFlag extends Flag<Float> {
    /** Creates a flag containing a {@link Float} value. */
    public static FloatFlag create(String name, Float defaultValue) {
      return new AutoValue_FlagManager_FloatFlag(name, defaultValue);
    }
  }

  /** {@link Flag} implementation for enum types. */
  @AutoValue
  abstract class EnumFlag<T extends Enum<T>> extends Flag<T> {
    public abstract Class<T> type();

    /** Creates a flag containing an enum value. */
    public static <T extends Enum<T>> EnumFlag<T> create(
        Class<T> clazz, String name, T defaultValue) {
      return new AutoValue_FlagManager_EnumFlag<>(name, defaultValue, clazz);
    }
  }

  /** {@link Flag} implementation for immutable lists of strings. */
  @AutoValue
  abstract class StringListFlag extends Flag<ImmutableList<String>> {
    public static StringListFlag create(String name, ImmutableList<String> defaultValue) {
      return new AutoValue_FlagManager_StringListFlag(name, defaultValue);
    }
  }
}
