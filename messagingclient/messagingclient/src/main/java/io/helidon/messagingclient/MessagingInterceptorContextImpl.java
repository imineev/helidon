/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.messagingclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.helidon.common.context.Context;

/**
 * Interceptor is a mutable object that is sent to {@link MessagingInterceptor}.
 */
class MessagingInterceptorContextImpl implements MessagingInterceptorContext {
    private final String messagingType;
    private Context context;
    private String channelName;
    private String channel;
    private CompletionStage<Void> channelFuture;
    private CompletionStage<HelidonMessage> queryFuture;
    private List<Object> indexedParams;
    private Map<String, Object> namedParams;
    private boolean indexed;

    MessagingInterceptorContextImpl(String messagingType) {
        this.messagingType = messagingType;
    }

    @Override
    public String messagingType() {
        return messagingType;
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public String channelName() {
        return channelName;
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public CompletionStage<Void> channelFuture() {
        return channelFuture;
    }

    @Override
    public Optional<List<Object>> indexedParameters() {
        if (indexed) {
            return Optional.of(indexedParams);
        }
        throw new IllegalStateException("Indexed parameters are not available for channel with named parameters");
    }

    @Override
    public Optional<Map<String, Object>> namedParameters() {
        if (indexed) {
            throw new IllegalStateException("Named parameters are not available for channel with indexed parameters");
        }
        return Optional.of(namedParams);
    }

    @Override
    public boolean isIndexed() {
        return indexed;
    }

    @Override
    public boolean isNamed() {
        return !indexed;
    }

    @Override
    public MessagingInterceptorContext context(Context context) {
        this.context = context;
        return this;
    }

    @Override
    public MessagingInterceptorContext channelName(String newName) {
        this.channelName = newName;
        return this;
    }

    @Override
    public MessagingInterceptorContext channelFuture(CompletionStage<Void> channelFuture) {
        this.channelFuture = channelFuture;
        return this;
    }

    @Override
    public CompletionStage<HelidonMessage> resultFuture() {
        return queryFuture;
    }

    @Override
    public MessagingInterceptorContext resultFuture(CompletionStage<HelidonMessage> resultFuture) {
        this.queryFuture = resultFuture;
        return this;
    }

    @Override
    public MessagingInterceptorContext channel(String channel, List<Object> indexedParams) {
        this.channel = channel;
        this.indexedParams = indexedParams;
        this.indexed = true;
        return this;
    }

    @Override
    public MessagingInterceptorContext channel(String channel, Map<String, Object> namedParams) {
        this.channel = channel;
        this.namedParams = namedParams;
        this.indexed = false;
        return this;
    }

    @Override
    public MessagingInterceptorContext parameters(List<Object> indexedParameters) {
        if (indexed) {
            this.indexedParams = indexedParameters;
        } else {
            throw new IllegalStateException("Cannot configure indexed parameters for a channel that expects named "
                                                    + "parameters");
        }
        return this;
    }

    @Override
    public MessagingInterceptorContext parameters(Map<String, Object> namedParameters) {
        if (indexed) {
            throw new IllegalStateException("Cannot configure named parameters for a channel that expects indexed "
                                                    + "parameters");
        }

        this.namedParams = namedParameters;
        return this;
    }
}
