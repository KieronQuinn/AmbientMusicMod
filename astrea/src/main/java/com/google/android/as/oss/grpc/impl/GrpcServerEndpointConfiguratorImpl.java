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

import static com.google.android.as.oss.grpc.impl.PcsSecurityPolicies.buildServerSecurityPolicy;
import static com.google.android.as.oss.grpc.impl.PcsSecurityPolicies.untrustedPolicy;

import android.content.Context;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.google.android.as.oss.grpc.GrpcServerEndpointConfiguration;
import com.google.android.as.oss.grpc.GrpcServerEndpointConfigurator;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.binder.AndroidComponentAddress;
import io.grpc.binder.BinderServerBuilder;
import io.grpc.binder.IBinderReceiver;
import io.grpc.binder.InboundParcelablePolicy;
import io.grpc.binder.ServerSecurityPolicy;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Configurator used to build an {@link IBinder} for exposing {@link BindableService}
 * implementations provided via a {@link GrpcServerEndpointConfiguration}.
 *
 * <p>TODO: Pick an executor for {@link BinderServerBuilder}.
 *
 * <p>TODO: Use LifecycleOnDestroyHelper to ensure any ongoing calls are cancelled if the service
 * goes down.
 */
@Module
@InstallIn(SingletonComponent.class)
final class GrpcServerEndpointConfiguratorImpl implements GrpcServerEndpointConfigurator {

  @Inject
  GrpcServerEndpointConfiguratorImpl() {}

  @Override
  @Nullable
  public IBinder buildOnDeviceServerEndpoint(
      Context context, Class<?> cls, GrpcServerEndpointConfiguration configuration)
      throws IOException {
    IBinderReceiver iBinderReceiver = new IBinderReceiver();
    buildAndStartOnDeviceServer(context, cls, configuration, iBinderReceiver);
    return iBinderReceiver.get();
  }

  private void buildAndStartOnDeviceServer(
      Context context,
      Class<?> cls,
      GrpcServerEndpointConfiguration configuration,
      IBinderReceiver iBinderReceiver)
      throws IOException {

    // TODO: Use allowlistedOnly() instead of untrustedPolicy().
    ServerSecurityPolicy serverSecurityPolicy =
        buildServerSecurityPolicy(untrustedPolicy(), configuration);

    BinderServerBuilder builder =
        BinderServerBuilder.forAddress(
                AndroidComponentAddress.forLocalComponent(context, cls), iBinderReceiver)
            .securityPolicy(serverSecurityPolicy)
            .inboundParcelablePolicy(buildInboundParcelablePolicy())
            .intercept(new MetadataExtractionServerInterceptor())
            // Disable compression by default, since there's little benefit when all communication
            // is
            // on-device, and it means sending supported-encoding headers with every call.
            .decompressorRegistry(DecompressorRegistry.emptyInstance())
            .compressorRegistry(CompressorRegistry.newEmptyInstance());

    for (BindableService service : configuration.getServices()) {
      builder.addService(service);
    }
    builder.build().start();
  }

  private InboundParcelablePolicy buildInboundParcelablePolicy() {
    return InboundParcelablePolicy.newBuilder().setAcceptParcelableMetadataValues(true).build();
  }
}
