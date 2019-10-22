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
import io.helidon.messagingclient.spi.MessagingClientProvider;

/**
 * Interceptor context to get (and possibly manipulate) messaging channels.
 * <p>
 * This is a mutable object - acts as a builder during the invocation of {@link MessagingInterceptor}.
 * The interceptors are executed sequentially, so there is no need for synchronization.
 */
public interface MessagingInterceptorContext {
    /**
     * Create a new interceptor context for a messaging provider.
     *
     * @param messagingType a short name of the messaging type (such as jms:AQ)
     * @return a new interceptor context ready to be configured
     */
    static MessagingInterceptorContext create(String messagingType) {
        return new MessagingInterceptorContextImpl(messagingType);
    }

    /**
     * Type of this messaging (usually the same string used by the {@link MessagingClientProvider#name()}).
     *
     * @return type of messaging
     */
    String messagingType();

    /**
     * Context with parameters passed from the caller, such as {@code SpanContext} for tracing.
     *
     * @return context associated with this request
     */
    Context context();

    /**
     * Name of a channel to be executed.
     * Ad hoc channels have names generated.
     *
     * @return name of the channel
     */
    String channelName();

    /**
     * Text of the channel to be executed.
     *
     * @return channel text
     */
    String channel();

    /**
     * A stage that is completed once the channel finishes execution.
     *
     * @return channel future
     */
    CompletionStage<Void> channelFuture();

    /**
     * A stage that is completed once the results were fully read. The number returns either the number of modified
     * records or the number of records actually read.
     *
     * @return stage that completes once all query results were processed.
     */
    CompletionStage<Message> resultFuture();

    /**
     * Indexed parameters (if used).
     *
     * @return indexed parameters (empty if this channel parameters are not indexed)
     */
    Optional<List<Object>> indexedParameters();

    /**
     * Named parameters (if used).
     *
     * @return named parameters (empty if this channel parameters are not named)
     */
    Optional<Map<String, Object>> namedParameters();

    /**
     * Whether this is a channel with indexed parameters.
     *
     * @return Whether this channel has indexed parameters ({@code true}) or named parameters {@code false}.
     */
    boolean isIndexed();

    /**
     * Whether this is a channel with named parameters.
     *
     * @return Whether this channel has named parameters ({@code true}) or indexed parameters {@code false}.
     */
    boolean isNamed();

    /**
     * Set a new context to be used by other interceptors and when executing the channel.
     *
     * @param context context to use
     * @return updated interceptor context
     */
    MessagingInterceptorContext context(Context context);

    /**
     * Set a new channel name to be used.
     *
     * @param newName channel name to use
     * @return updated interceptor context
     */
    MessagingInterceptorContext channelName(String newName);

    /**
     * Set a new future to mark completion of the channel.
     *
     * @param channelFuture future
     * @return updated interceptor context
     */
    MessagingInterceptorContext channelFuture(CompletionStage<Void> channelFuture);

    /**
     * Set a new future to mark completion of the result (e.g. query or number of modified records).
     *
     * @param queryFuture future
     * @return updated interceptor context
     */
    MessagingInterceptorContext resultFuture(CompletionStage<Message> queryFuture);

    /**
     * Set a new channel with indexed parameters to be used.
     *
     * @param channel     channel text
     * @param indexedParams indexed parameters
     * @return updated interceptor context
     */
    MessagingInterceptorContext channel(String channel, List<Object> indexedParams);

    /**
     * Set a new channel with named parameters to be used.
     *
     * @param channel   channel text
     * @param namedParams named parameters
     * @return updated interceptor context
     */
    MessagingInterceptorContext channel(String channel, Map<String, Object> namedParams);

    /**
     * Set new indexed parameters to be used.
     *
     * @param indexedParameters parameters
     * @return updated interceptor context
     * @throws IllegalArgumentException in case the channel is using named parameters
     */
    MessagingInterceptorContext parameters(List<Object> indexedParameters);

    /**
     * Set new named parameters to be used.
     *
     * @param namedParameters parameters
     * @return updated interceptor context
     * @throws IllegalArgumentException in case the channel is using indexed parameters
     */
    MessagingInterceptorContext parameters(Map<String, Object> namedParameters);

}
