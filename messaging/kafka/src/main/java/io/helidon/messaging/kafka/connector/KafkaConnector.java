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

package io.helidon.messaging.kafka.connector;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.messaging.kafka.KafkaConsumer;
import io.helidon.messaging.kafka.KafkaProducer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KafkaConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {

    public static final String CONNECTOR_NAME = "helidon-kafka";
    private ThreadPoolSupplier threadPoolSupplier = null;

    private List<KafkaConsumer<Object, Object>> consumers = new CopyOnWriteArrayList<>();
    private List<KafkaProducer<Object, Object>> producers = new CopyOnWriteArrayList<>();

    public List<KafkaConsumer<Object, Object>> getConsumers() {
        return consumers;
    }
    public List<KafkaProducer<Object, Object>> getProducers() {
        return producers;
    }


    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        KafkaConsumer<Object, Object> kafkaConsumer = new KafkaConsumer<>(config);
        consumers.add(kafkaConsumer);
        return kafkaConsumer.createPublisherBuilder(ThreadPoolSupplier.create().get());
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        KafkaProducer<Object, Object> kafkaProducer =  new KafkaProducer<>(config);
        producers.add(kafkaProducer);
        return kafkaProducer.createSubscriberBuilder(ThreadPoolSupplier.create().get());
    }

}
