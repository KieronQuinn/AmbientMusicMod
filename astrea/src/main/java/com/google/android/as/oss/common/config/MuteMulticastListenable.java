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

import java.util.concurrent.Executor;

/**
 * A listenable that does not accept any listeners.
 *
 * <p>This type is handy to avoid using null in cases where we don't need to notify.
 * (https://github.com/google/guava/wiki/UsingAndAvoidingNullExplained)
 */
// See [redacted]
@SuppressWarnings("ExtendsObject")
public class MuteMulticastListenable<L extends Object> extends MulticastListenable<L> {
  @Override
  public boolean addListener(L listener) {
    return false;
  }

  @Override
  public boolean removeListener(L listener) {
    return false;
  }

  @Override
  public void notify(Consumer<L> function) {
    // We got nothing to say.
  }

  private MuteMulticastListenable(Executor notifyExecutor) {
    super(notifyExecutor);
  }
}
