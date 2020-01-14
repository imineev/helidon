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

package io.helidon.messaging.jms.connector;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.messaging.jms.JMSConsumer;
import io.helidon.messaging.jms.JMSProducer;
import io.helidon.microprofile.config.MpConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;
import java.util.stream.Collector;


public class JMSConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {

    public static final String CONNECTOR_NAME = "helidon-jms";
    private ThreadPoolSupplier threadPoolSupplier = null;

    private List<JMSConsumer<Object, Object>> consumers = new CopyOnWriteArrayList<>();
    private List<JMSProducer<Object, Object>> producers = new CopyOnWriteArrayList<>();

    public List<JMSConsumer<Object, Object>> getConsumers() {
        return consumers;
    }
    public List<JMSProducer<Object, Object>> getProducers() {
        return producers;
    }


    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        JMSConsumer<Object, Object> JMSConsumer = new JMSConsumer<>(config);
        return JMSConsumer.createPublisherBuilder(ThreadPoolSupplier.create().get());
//        return JMSConsumer.createPublisherBuilder(getThreadPoolSupplier(((MpConfig) config).helidonConfig()).get());
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        JMSProducer<Object, Object> JMSProducer =  new JMSProducer<>(config);
        producers.add(JMSProducer);
        return JMSProducer.createSubscriberBuilder(ThreadPoolSupplier.create().get());
//        return JMSProducer.createSubscriberBuilder(getThreadPoolSupplier(((MpConfig) config).helidonConfig()));
    }

    private ThreadPoolSupplier getThreadPoolSupplier(io.helidon.config.Config config) {
        synchronized (this) {
            if (this.threadPoolSupplier != null) {
                return this.threadPoolSupplier;
            }
            this.threadPoolSupplier = ThreadPoolSupplier.create(config.get("executor-service"));
            return threadPoolSupplier;
        }
    }

}
