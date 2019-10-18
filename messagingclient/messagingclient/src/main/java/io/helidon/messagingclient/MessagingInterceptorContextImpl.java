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
    private String operationName;
    private String operation;
    private CompletionStage<Void> operationFuture;
    private CompletionStage<Message> queryFuture;
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
    public String operationName() {
        return operationName;
    }

    @Override
    public String operation() {
        return operation;
    }

    @Override
    public CompletionStage<Void> operationFuture() {
        return operationFuture;
    }

    @Override
    public Optional<List<Object>> indexedParameters() {
        if (indexed) {
            return Optional.of(indexedParams);
        }
        throw new IllegalStateException("Indexed parameters are not available for operation with named parameters");
    }

    @Override
    public Optional<Map<String, Object>> namedParameters() {
        if (indexed) {
            throw new IllegalStateException("Named parameters are not available for operation with indexed parameters");
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
    public MessagingInterceptorContext operationName(String newName) {
        this.operationName = newName;
        return this;
    }

    @Override
    public MessagingInterceptorContext operationFuture(CompletionStage<Void> operationFuture) {
        this.operationFuture = operationFuture;
        return this;
    }

    @Override
    public CompletionStage<Message> resultFuture() {
        return queryFuture;
    }

    @Override
    public MessagingInterceptorContext resultFuture(CompletionStage<Message> resultFuture) {
        this.queryFuture = resultFuture;
        return this;
    }

    @Override
    public MessagingInterceptorContext operation(String operation, List<Object> indexedParams) {
        this.operation = operation;
        this.indexedParams = indexedParams;
        this.indexed = true;
        return this;
    }

    @Override
    public MessagingInterceptorContext operation(String operation, Map<String, Object> namedParams) {
        this.operation = operation;
        this.namedParams = namedParams;
        this.indexed = false;
        return this;
    }

    @Override
    public MessagingInterceptorContext parameters(List<Object> indexedParameters) {
        if (indexed) {
            this.indexedParams = indexedParameters;
        } else {
            throw new IllegalStateException("Cannot configure indexed parameters for a operation that expects named "
                                                    + "parameters");
        }
        return this;
    }

    @Override
    public MessagingInterceptorContext parameters(Map<String, Object> namedParameters) {
        if (indexed) {
            throw new IllegalStateException("Cannot configure named parameters for a operation that expects indexed "
                                                    + "parameters");
        }

        this.namedParams = namedParameters;
        return this;
    }
}
