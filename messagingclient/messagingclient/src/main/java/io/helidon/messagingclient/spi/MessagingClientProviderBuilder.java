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
package io.helidon.messagingclient.spi;

import io.helidon.common.Builder;
import io.helidon.config.Config;
import io.helidon.messagingclient.*;
import io.helidon.messagingclient.MessagingInterceptor;

/**
 * messaging provider builder.
 *
 * @param <T> type of the builder extending implementing this interface.
 */
public interface MessagingClientProviderBuilder<T extends MessagingClientProviderBuilder<T>> extends Builder<MessagingClient> {

    /**
     * Use messaging connection configuration from configuration file.
     *
     * @param config {@link io.helidon.config.Config} instance with messaging connection attributes
     * @return messaging provider builder
     */
    T config(Config config);

    /**
     * Set messaging connection string (URL).
     *
     * @param url messaging connection string
     * @return messaging provider builder
     */
    T url(String url);

    /**
     * Set messaging connection user name.
     *
     * @param username messaging connection user name
     * @return messaging provider builder
     */
    T username(String username);

    /**
     * Set messaging connection p¨assword.
     *
     * @param password messaging connection password
     * @return messaging provider builder
     */
    T password(String password);

    /**
     * Add an interceptor.
     * This allows to add implementation of tracing, metrics, logging etc. without the need to hard-code these into
     * the base.
     *
     * @param interceptor interceptor instance
     * @return updated builder instance
     */
    T addInterceptor(MessagingInterceptor interceptor);

    /**
     * Add an interceptor that is active only on the configured operation names.
     * This interceptor is only executed on named operations.
     *
     * @param interceptor interceptor instance
     * @param operationNames operation names to be active on
     * @return updated builder instance
     */
    T addInterceptor(MessagingInterceptor interceptor, MessagingOperationType... operationNames);

    /**
     * Add an interceptor that is active only on the configured operation names.
     * This interceptor is only executed on named operations.
     *
     * @param interceptor interceptor instance
     * @param operationNames operation names to be active on
     * @return updated builder instance
     */
    T addInterceptor(MessagingInterceptor interceptor, String... operationNames);


    /**
     * Build messaging handler for specific provider.
     *
     * @return messaging handler instance
     */
    @Override
    MessagingClient build();

    void topic(String s);

    void queue(String s);
}
