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
package io.helidon.messagingclient.common;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.helidon.common.CollectionsHelper;
import io.helidon.common.configurable.LruCache;
import io.helidon.messagingclient.MessagingInterceptor;
import io.helidon.messagingclient.MessagingOperationType;

/**
 * Support for interceptors.
 */
public interface InterceptorSupport {
    /**
     * Create a new fluent API builder.
     *
     * @return a builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Get a list of interceptors to be executed for the specified operation.
     *
     * @param MessagingOperationType Type of the operation
     * @param operationName          Name of the operation (unnamed operations should have a name generated, see
     * @return list of interceptors to executed for the defined type and name (may be empty)
     * @see io.helidon.messagingclient.MessagingInterceptor
     */
    List<MessagingInterceptor> interceptors(MessagingOperationType MessagingOperationType, String operationName);

    /**
     * Fluent API builder for {@link io.helidon.messagingclient.common.InterceptorSupport}.
     */
    final class Builder implements io.helidon.common.Builder<InterceptorSupport> {
        private final List<MessagingInterceptor> interceptors = new LinkedList<>();
        private final Map<String, List<MessagingInterceptor>> namedOperationInterceptors = new HashMap<>();
        private final Map<MessagingOperationType, List<MessagingInterceptor>> typeInterceptors =
                new EnumMap<>(MessagingOperationType.class);

        private Builder() {
        }


        @Override
        public InterceptorSupport build() {
            // the result must be immutable (if somebody modifies the builder, the behavior must not change)
            List<MessagingInterceptor> interceptors = new LinkedList<>(this.interceptors);
            final Map<MessagingOperationType, List<MessagingInterceptor>> typeInterceptors =
                    new EnumMap<>(this.typeInterceptors);
            final Map<String, List<MessagingInterceptor>> namedOperationInterceptors = new HashMap<>(this.namedOperationInterceptors);

            final LruCache<CacheKey, List<MessagingInterceptor>> cachedInterceptors = LruCache.create();
            return new InterceptorSupport() {
                @Override
                public List<MessagingInterceptor> interceptors(MessagingOperationType MessagingOperationType, String operationName) {
                    // order is defined in MessagingInterceptor interface
                    return cachedInterceptors.computeValue(new CacheKey(MessagingOperationType, operationName), () -> {
                        List<MessagingInterceptor> result = new LinkedList<>();
                        addAll(result, namedOperationInterceptors.get(operationName));
                        addAll(result, typeInterceptors.get(MessagingOperationType));
                        result.addAll(interceptors);
                        return Optional.of(Collections.unmodifiableList(result));
                    }).orElseGet(CollectionsHelper::listOf);
                }

                private void addAll(List<MessagingInterceptor> result, List<MessagingInterceptor> MessagingInterceptors) {
                    if (null == MessagingInterceptors) {
                        return;
                    }
                    result.addAll(MessagingInterceptors);
                }
            };
        }

        public Builder add(MessagingInterceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        public Builder add(MessagingInterceptor interceptor, String... operationNames) {
            for (String operationName : operationNames) {
                this.namedOperationInterceptors.computeIfAbsent(operationName, theName -> new LinkedList<>())
                        .add(interceptor);
            }
            return this;
        }

        public Builder add(MessagingInterceptor interceptor, MessagingOperationType... dbOperationTypes) {
            for (MessagingOperationType dbOperationType : dbOperationTypes) {
                this.typeInterceptors.computeIfAbsent(dbOperationType, theType -> new LinkedList<>())
                        .add(interceptor);
            }
            return this;
        }

        private static final class CacheKey {
            private final MessagingOperationType type;
            private final String operationName;

            private CacheKey(MessagingOperationType type, String operationName) {
                this.type = type;
                this.operationName = operationName;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof CacheKey)) {
                    return false;
                }
                CacheKey cacheKey = (CacheKey) o;
                return (type == cacheKey.type)
                        && operationName.equals(cacheKey.operationName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, operationName);
            }
        }
    }
}
