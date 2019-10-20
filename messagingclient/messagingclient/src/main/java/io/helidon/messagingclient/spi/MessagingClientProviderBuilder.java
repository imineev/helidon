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
 * Database provider builder.
 *
 * @param <T> type of the builder extending implementing this interface.
 */
public interface MessagingClientProviderBuilder<T extends MessagingClientProviderBuilder<T>> extends Builder<MessagingClient> {

    /**
     * Use database connection configuration from configuration file.
     *
     * @param config {@link io.helidon.config.Config} instance with database connection attributes
     * @return database provider builder
     */
    T config(Config config);

    /**
     * Set database connection string (URL).
     *
     * @param url database connection string
     * @return database provider builder
     */
    T url(String url);

    /**
     * Set database connection user name.
     *
     * @param username database connection user name
     * @return database provider builder
     */
    T username(String username);

    /**
     * Set database connection p¨assword.
     *
     * @param password database connection password
     * @return database provider builder
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
     * Build database handler for specific provider.
     *
     * @return database handler instance
     */
    @Override
    MessagingClient build();

    void topic(String s);

    void queue(String s);
}
