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

import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.helidon.common.serviceloader.HelidonServiceLoader;
import io.helidon.config.Config;
import io.helidon.config.ConfigValue;
import io.helidon.messagingclient.spi.MessagingClientProvider;
import io.helidon.messagingclient.spi.MessagingClientProviderBuilder;
import io.helidon.messagingclient.spi.MessagingInterceptorProvider;

/**
 * Helidon messaging client.
 */
public interface MessagingClient {

    static MessagingChannels builder(org.eclipse.microprofile.config.Config mpConfig) {
        return new MessagingChannels.Builder().build();
    }

    /**
     * Pings the messaging, completes when Messagingis up and ready, completes exceptionally if not.
     *
     * @return stage that completes when the ping finished
     */
    CompletionStage<Void> ping();

    /**
     * Type of this messaging provider (such as jms:AQ, kafka, etc..).
     *
     * @return name of the messaging provider
     */
    String messagingType();

    /**
     * Create Helidon messaging handler builder.
     *
     * @param config name of the configuration node with driver configuration
     * @return messaging handler builder
     */
    static MessagingClient create(Config config) {
        return builder(config).build();
    }

    /**
     * channel for messages.
     *
     * @param <T>      channel result type
     * @param executor messaging channel executor, see {@link MessagingChannelOptions}
     * @return channel result
     */
    <T> T channel(Function<MessagingChannelOptions, T> executor);

    /**
     * Create Helidon messaging handler builder.
     * <p>messaging driver is loaded as SPI provider which implements {@link MessagingClientProvider} interface.
     * First provider on the class path is selected.</p>
     *
     * @return messaging handler builder
     */
    static Builder builder() {
        MessagingClientProvider theSource = MessagingClientProviderLoader.first();
        if (null == theSource) {
            throw new MessagingClientException(
                    "No MessagingSource defined on classpath/module path. " +
                            "An implementation of io.helidon.messagingclient.spi.MessagingSource is required "
                            + "to access a messaging");
        }

        return builder(theSource);
    }

    /**
     * Create Helidon messaging handler builder.
     *
     * @param source messaging driver
     * @return messaging handler builder
     */
    static Builder builder(MessagingClientProvider source) {
        return new Builder(source);
    }

    /**
     * Create Helidon messaging handler builder.
     * <p>messaging driver is loaded as SPI provider which implements {@link MessagingClientProvider} interface.
     * Provider on the class path with matching name is selected.</p>
     *
     * @param messagingSource SPI provider name
     * @return messaging handler builder
     */
    static Builder builder(String messagingSource) {

        return MessagingClientProviderLoader.get(messagingSource)
                .map(MessagingClient::builder)
                .orElseThrow(() -> new MessagingClientException(
                        "No MessasgingSource defined on classpath/module path for name: "
                                + messagingSource
                                + ", available names: " + Arrays.toString(MessagingClientProviderLoader.names())));
    }

    /**
     * Create a Helidon messaging handler builder from configuration.
     *
     * @param messagingConfig configuration that should contain the key {@code source} that defines the type of this messaging
     *                 and is used to load appropriate {@link MessagingClientProvider} from Java Service loader
     * @return a builder pre-configured from the provided config
     */
    static Builder builder(Config messagingConfig) {
        return messagingConfig.get("source")
                .asString()
                // use builder for correct MessagingSource
                .map(MessagingClient::builder)
                // or use the default one
                .orElseGet(MessagingClient::builder)
                .config(messagingConfig);
    }

    /**
     * Helidon messaging handler builder.
     */
    final class Builder implements io.helidon.common.Builder<MessagingClient> {
        private final HelidonServiceLoader.Builder<MessagingInterceptorProvider> interceptorServices = HelidonServiceLoader.builder(
                ServiceLoader.load(MessagingInterceptorProvider.class));

        /**
         * Provider specific messaging handler builder instance.
         */
        private final MessagingClientProviderBuilder<?> theBuilder;
        private Config config;

        /**
         * Create an instance of Helidon messaging handler builder.
         *
         * @param messagingClientProvider provider specific {@link MessagingClientProvider} instance
         */
        private Builder(MessagingClientProvider messagingClientProvider) {
            this.theBuilder = messagingClientProvider.builder();
        }

        /**
         * Build provider specific messaging handler.
         *
         * @return new messaging handler instance
         */
        @Override
        public MessagingClient build() {
            // add interceptors from service loader
            if (null != config) {
                Config interceptors = config.get("interceptors");
                List<MessagingInterceptorProvider> providers = interceptorServices.build().asList();
                for (MessagingInterceptorProvider provider : providers) {
                    Config providerConfig = interceptors.get(provider.configKey());
                    if (!providerConfig.exists()) {
                        continue;
                    }
                    // if configured, we want to at least add a global one
                    AtomicBoolean added = new AtomicBoolean(false);
                    Config global = providerConfig.get("global");
                    if (global.exists() && !global.isLeaf()) {
                        // we must iterate through nodes
                        global.asNodeList().ifPresent(configs -> {
                            configs.forEach(globalConfig -> {
                                added.set(true);
                                addInterceptor(provider.create(globalConfig));
                            });
                        });
                    }

                    Config named = providerConfig.get("named");
                    if (named.exists()) {
                        // we must iterate through nodes
                        named.asNodeList().ifPresent(configs -> {
                            configs.forEach(namedConfig -> {
                                ConfigValue<List<String>> names = namedConfig.get("names").asList(String.class);
                                names.ifPresent(nameList -> {
                                    added.set(true);
                                    addInterceptor(provider.create(namedConfig), nameList.toArray(new String[0]));
                                });
                            });
                        });
                    }
                    //
                    if (!added.get()) {
                        if (global.exists()) {
                            addInterceptor(provider.create(global));
                        } else {
                            addInterceptor(provider.create(providerConfig));
                        }
                    }
                }
            }

            return theBuilder.build();
        }

        /**
         * Add an interceptor provider.
         * The provider is only used when configuration is used ({@link #config(io.helidon.config.Config)}.
         *
         * @param provider provider to add to the list of loaded providers
         * @return updated builder instance
         */
        public Builder addInterceptorProvider(MessagingInterceptorProvider provider) {
            this.interceptorServices.addService(provider);
            return this;
        }

        /**
         * Add a global interceptor.
         * <p>
         * A global interceptor is applied to each channel.
         *
         * @param interceptor interceptor to apply
         * @return updated builder instance
         */
        public Builder addInterceptor(MessagingInterceptor interceptor) {
            theBuilder.addInterceptor(interceptor);
            return this;
        }

        /**
         * Add an interceptor to specific named channels.
         *
         * @param interceptor    interceptor to apply
         * @param channelNames names of channels to apply it on
         * @return updated builder instance
         */
        public Builder addInterceptor(MessagingInterceptor interceptor, MessagingChannelType... channelNames) {
            theBuilder.addInterceptor(interceptor, channelNames);
            return this;
        }

        /**
         * Add an interceptor to specific named channels.
         *
         * @param interceptor    interceptor to apply
         * @param filterNames names of channels to apply it on
         * @return updated builder instance
         */
        public Builder addInterceptor(MessagingInterceptor interceptor, String... filterNames) {
            theBuilder.addInterceptor(interceptor, filterNames);
            return this;
        }

        /**
         * Use messaging connection configuration from configuration file.
         *
         * @param config {@link io.helidon.config.Config} instance with messaging connection attributes
         * @return messaging provider builder
         */
        public Builder config(Config config) {
            theBuilder.config(config);

            this.config = config;

            return this;
        }


    }

}
