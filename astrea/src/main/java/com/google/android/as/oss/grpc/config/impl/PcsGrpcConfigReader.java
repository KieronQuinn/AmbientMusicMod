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

package com.google.android.as.oss.grpc.config.impl;

import com.google.android.as.oss.common.config.AbstractConfigReader;
import com.google.android.as.oss.common.config.FlagListener;
import com.google.android.as.oss.common.config.FlagManager;
import com.google.android.as.oss.common.config.FlagManager.IntegerFlag;
import com.google.android.as.oss.grpc.config.PcsGrpcConfig;

/** ConfigReader for {@link PcsGrpcConfig}. */
class PcsGrpcConfigReader extends AbstractConfigReader<PcsGrpcConfig> {
  private static final String FLAG_PREFIX = "PcsGrpc__";

  static final IntegerFlag IDLE_TIMEOUT_SECONDS =
      IntegerFlag.create("PcsGrpc__idle_timeout_seconds", 60);

  private final FlagManager flagManager;

  static PcsGrpcConfigReader create(FlagManager flagManager) {
    PcsGrpcConfigReader instance = new PcsGrpcConfigReader(flagManager);

    instance
        .flagManager
        .listenable()
        .addListener(
            (flagNames) -> {
              if (FlagListener.anyHasPrefix(flagNames, FLAG_PREFIX)) {
                instance.refreshConfig();
              }
            });

    return instance;
  }

  @Override
  protected PcsGrpcConfig computeConfig() {
    return PcsGrpcConfig.builder()
        .setIdleTimeoutSeconds(flagManager.get(IDLE_TIMEOUT_SECONDS))
        .build();
  }

  private PcsGrpcConfigReader(FlagManager flagManager) {
    this.flagManager = flagManager;
  }
}
