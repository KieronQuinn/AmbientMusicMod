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

package com.google.android.as.oss.networkusage.ui.content;

/** Thrown when a client sends an unrecognized network request to PCS. */
public class UnrecognizedNetworkRequestException extends Exception {

  public static UnrecognizedNetworkRequestException forUrl(String url) {
    return new UnrecognizedNetworkRequestException(
        String.format("Unrecognized request to PCS for url '%s'", url));
  }

  public static UnrecognizedNetworkRequestException forFeatureName(String featureName) {
    return new UnrecognizedNetworkRequestException(
        String.format("Unrecognized request to PCS for feature name '%s'", featureName));
  }

  private UnrecognizedNetworkRequestException(String description) {
    super(description);
  }
}
