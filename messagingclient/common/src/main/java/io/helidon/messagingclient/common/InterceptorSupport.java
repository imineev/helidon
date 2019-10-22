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
import io.helidon.messagingclient.MessagingChannelType;

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
     * Get a list of interceptors to be executed for the specified channel.
     *
     * @param MessagingChannelType Type of the channel
     * @param channelName          Name of the channel (unnamed channels should have a name generated, see
     * @return list of interceptors to executed for the defined type and name (may be empty)
     * @see io.helidon.messagingclient.MessagingInterceptor
     */
    List<MessagingInterceptor> interceptors(MessagingChannelType MessagingChannelType, String channelName);

    /**
     * Fluent API builder for {@link io.helidon.messagingclient.common.InterceptorSupport}.
     */
    final class Builder implements io.helidon.common.Builder<InterceptorSupport> {
        private final List<MessagingInterceptor> interceptors = new LinkedList<>();
        private final Map<String, List<MessagingInterceptor>> namedChannelInterceptors = new HashMap<>();
        private final Map<MessagingChannelType, List<MessagingInterceptor>> typeInterceptors =
                new EnumMap<>(MessagingChannelType.class);

        private Builder() {
        }


        @Override
        public InterceptorSupport build() {
            // the result must be immutable (if somebody modifies the builder, the behavior must not change)
            List<MessagingInterceptor> interceptors = new LinkedList<>(this.interceptors);
            final Map<MessagingChannelType, List<MessagingInterceptor>> typeInterceptors =
                    new EnumMap<>(this.typeInterceptors);
            final Map<String, List<MessagingInterceptor>> namedChannelInterceptors = new HashMap<>(this.namedChannelInterceptors);

            final LruCache<CacheKey, List<MessagingInterceptor>> cachedInterceptors = LruCache.create();
            return new InterceptorSupport() {
                @Override
                public List<MessagingInterceptor> interceptors(MessagingChannelType MessagingChannelType, String channelName) {
                    // order is defined in MessagingInterceptor interface
                    return cachedInterceptors.computeValue(new CacheKey(MessagingChannelType, channelName), () -> {
                        List<MessagingInterceptor> result = new LinkedList<>();
                        addAll(result, namedChannelInterceptors.get(channelName));
                        addAll(result, typeInterceptors.get(MessagingChannelType));
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

        public Builder add(MessagingInterceptor interceptor, String... channelNames) {
            for (String channelName : channelNames) {
                this.namedChannelInterceptors.computeIfAbsent(channelName, theName -> new LinkedList<>())
                        .add(interceptor);
            }
            return this;
        }

        public Builder add(MessagingInterceptor interceptor, MessagingChannelType... dbChannelTypes) {
            for (MessagingChannelType dbChannelType : dbChannelTypes) {
                this.typeInterceptors.computeIfAbsent(dbChannelType, theType -> new LinkedList<>())
                        .add(interceptor);
            }
            return this;
        }

        private static final class CacheKey {
            private final MessagingChannelType type;
            private final String channelName;

            private CacheKey(MessagingChannelType type, String channelName) {
                this.type = type;
                this.channelName = channelName;
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
                        && channelName.equals(cacheKey.channelName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, channelName);
            }
        }
    }
}
