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

package com.google.android.as.oss.grpc.impl;

import android.content.Context;
import com.google.android.as.oss.grpc.Annotations.GrpcServicePackageName;
import com.google.android.as.oss.grpc.Annotations.PcsGrpcServiceName;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.grpc.Channel;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.binder.AndroidComponentAddress;
import io.grpc.binder.BinderChannelBuilder;
import javax.inject.Singleton;

/** Provides a Singleton on-device GRPC channel for PCS connections. */
@Module
@InstallIn(SingletonComponent.class)
interface OnDeviceChannelModule {
  @Provides
  @Singleton
  static Channel providesOnDeviceChannel(
      @ApplicationContext Context context,
      @GrpcServicePackageName String pcsPackageName,
      @PcsGrpcServiceName String pcsGrpcServiceName) {
    return BinderChannelBuilder.forAddress(
            AndroidComponentAddress.forRemoteComponent(pcsPackageName, pcsGrpcServiceName),
            context.getApplicationContext())
        .securityPolicy(PcsSecurityPolicies.untrustedPolicy())
        // Disable compression by default, since there's little benefit when all communication is
        // on-device, and it means sending supported-encoding headers with every call.
        .decompressorRegistry(DecompressorRegistry.emptyInstance())
        .compressorRegistry(CompressorRegistry.newEmptyInstance())
        .build();
  }
}
