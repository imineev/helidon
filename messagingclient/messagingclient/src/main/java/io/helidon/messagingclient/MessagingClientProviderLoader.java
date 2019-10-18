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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import io.helidon.common.serviceloader.HelidonServiceLoader;
import io.helidon.messagingclient.spi.MessagingClientProvider;

/**
 * Loads database client providers from Java Service loader.
 */
final class MessagingClientProviderLoader {
    private static final Map<String, MessagingClientProvider> Messaging_SOURCES = new HashMap<>();
    private static final String[] NAMES;
    private static final MessagingClientProvider FIRST;

    static {
        System.out.println("MessagingClientProviderLoader.static initializer");
        HelidonServiceLoader<MessagingClientProvider> serviceLoader = HelidonServiceLoader
                .builder(ServiceLoader.load(MessagingClientProvider.class))
                .build();

        List<MessagingClientProvider> sources = serviceLoader.asList();

        MessagingClientProvider first = null;

        if (!sources.isEmpty()) {
            first = sources.get(0);
        }

        FIRST = first;
        sources.forEach(messagingProvider -> Messaging_SOURCES.put(messagingProvider.name(), messagingProvider));
        NAMES = sources.stream()
                .map(MessagingClientProvider::name)
                .toArray(String[]::new);
    }

    private MessagingClientProviderLoader() {
    }

    static MessagingClientProvider first() {
        return FIRST;
    }

    static Optional<MessagingClientProvider> get(String name) {
        return Optional.ofNullable(Messaging_SOURCES.get(name));
    }

    static String[] names() {
        return NAMES;
    }
}
