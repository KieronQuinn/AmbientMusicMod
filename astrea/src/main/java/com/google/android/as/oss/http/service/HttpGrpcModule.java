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

package com.google.android.as.oss.http.service;

import com.google.android.apps.miphone.astrea.http.api.proto.HttpServiceGrpc;
import com.google.android.as.oss.grpc.Annotations.GrpcService;
import com.google.android.as.oss.grpc.Annotations.GrpcServiceName;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;
import io.grpc.BindableService;
import java.time.Duration;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
abstract class HttpGrpcModule {
  private static final Duration DEFAULT_HTTP_CONN_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration DEFAULT_HTTP_READ_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration DEFAULT_HTTP_WRITE_TIMEOUT = Duration.ofSeconds(60);
  private static final boolean DEFAULT_HTTP_RETRY_ON_CONN_FAILURE = true;

  @Binds
  @IntoSet
  @GrpcService
  abstract BindableService bindBindableService(HttpGrpcBindableService httpGrpcBindableService);

  @Provides
  @IntoSet
  @GrpcServiceName
  static String provideServiceName() {
    return HttpServiceGrpc.SERVICE_NAME;
  }

  @Provides
  static OkHttpClient provideHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(DEFAULT_HTTP_CONN_TIMEOUT)
        .readTimeout(DEFAULT_HTTP_READ_TIMEOUT)
        .writeTimeout(DEFAULT_HTTP_WRITE_TIMEOUT)
        .retryOnConnectionFailure(DEFAULT_HTTP_RETRY_ON_CONN_FAILURE)
        .build();
  }
}
