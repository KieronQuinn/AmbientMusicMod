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

package com.google.android.as.oss.grpc;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import com.google.common.flogger.GoogleLogger;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.IOException;
import javax.inject.Inject;

/** Service providing GRPC connection to PCS. */
@AndroidEntryPoint(LifecycleService.class)
public class AstreaGrpcService extends Hilt_AstreaGrpcService {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  @Inject GrpcServerEndpointConfiguration configuration;
  @Inject GrpcServerEndpointConfigurator configurator;

  @Nullable private IBinder binder;

  @Override
  public void onCreate() {
    super.onCreate();
    logger.atInfo().log(
        "AstreaGrpcService#onCreate called with the following services: %s",
        configuration.getServiceNames());

    try {
      binder = configurator.buildOnDeviceServerEndpoint(this, getClass(), configuration);
      if (binder == null) {
        throw new NullPointerException();
      }
    } catch (IOException | NullPointerException e) {
      throw new PCSBinderException("Failed to start grpc server.", e);
    }
  }

  @Override
  @Nullable
  public IBinder onBind(Intent intent) {
    super.onBind(intent);
    return binder;
  }

  /** Thrown if the GRPC server fails to start. */
  static class PCSBinderException extends RuntimeException {
    public PCSBinderException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
