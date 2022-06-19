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
 * Constants related to flag management that may be shared between different features. Part of it
 * proxies DeviceConfig-level constants, but referring to this class instead confines all direct
 * references to the specific framework to one package (i.e. easier to refactor).
 */
public enum FlagNamespace {
  DEVICE_PERSONALIZATION_SERVICES("device_personalization_services");

  private final String namespace;

  FlagNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Override
  public String toString() {
    return namespace;
  }
}
