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

import static com.google.android.as.oss.grpc.ContextKeys.WRITEABLE_FILE_CONTEXT_KEY;
import static com.google.android.as.oss.grpc.ContextKeys.WRITEABLE_FILE_METADATA_KEY;

import androidx.annotation.VisibleForTesting;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * {@link ServerInterceptor} for extracting metadata headers sent to server.
 *
 * <p>This class by default extracts a set of allowlisted metadata values and passes them along to
 * context. Bindable services can then read it directly from Context.currentContext().
 */
@VisibleForTesting
public class MetadataExtractionServerInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    Context context = Context.current();

    if (headers.containsKey(WRITEABLE_FILE_METADATA_KEY)) {
      context =
          context.withValue(WRITEABLE_FILE_CONTEXT_KEY, headers.get(WRITEABLE_FILE_METADATA_KEY));
    }

    return Contexts.interceptCall(context, call, headers, next);
  }
}
