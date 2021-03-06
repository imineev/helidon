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

package io.helidon.messaging.kafka;

import io.helidon.config.Config;
import io.helidon.messaging.kafka.connector.KafkaMessage;
import io.helidon.messaging.kafka.connector.KafkaPublisherBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Simple Kafka consumer covering basic use-cases.
 * Configurable by Helidon {@link io.helidon.config.Config Config},
 * For more info about configuration see {@link KafkaConfigProperties}
 * <p/>
 * Usage:
 * <pre>{@code
 *   try (KafkaConsumer<Long, String> c = new KafkaConsumer<>("test-channel", Config.create())) {
 *         c.consumeAsync(r -> System.out.println(r.value()));
 *   }
 * }</pre>
 *
 * @param <K> Key type
 * @param <V> Value type
 * @see KafkaConfigProperties
 * @see io.helidon.config.Config
 */
public class KafkaConsumer<K, V> implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumer.class.getName());
    private final KafkaConfigProperties properties;

    private AtomicBoolean closed = new AtomicBoolean(false);
    private PartitionsAssignedLatch partitionsAssignedLatch = new PartitionsAssignedLatch();
    private String consumerId;
    private ExecutorService executorService;
    private ExecutorService externalExecutorService;
    private List<String> topicNameList;
    private org.apache.kafka.clients.consumer.KafkaConsumer consumer;

    /**
     * Kafka consumer created from {@link io.helidon.config.Config config}
     * see configuration {@link KafkaConfigProperties example}.
     *
     * @param channelName key in configuration
     * @param config      Helidon {@link io.helidon.config.Config config}
     * @see KafkaConfigProperties
     * @see io.helidon.config.Config
     */
    public KafkaConsumer(String channelName, Config config) {
        this(channelName, config, null);
    }

    /**
     * Kafka consumer created from {@link io.helidon.config.Config config}
     * see configuration {@link KafkaConfigProperties example}.
     *
     * @param channelName     key in configuration
     * @param config          Helidon {@link io.helidon.config.Config config}
     * @param consumerGroupId Custom group.id, can be null, overrides group.id from configuration
     * @see KafkaConfigProperties
     * @see io.helidon.config.Config
     */
    public KafkaConsumer(String channelName, Config config, String consumerGroupId) {
        properties = new KafkaConfigProperties(config.get("mp.messaging.incoming").get(channelName));
        properties.setProperty(KafkaConfigProperties.GROUP_ID, getOrGenerateGroupId(consumerGroupId));
        this.topicNameList = properties.getTopicNameList();
        this.consumerId = channelName;
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer(properties);
    }

    public KafkaConsumer(org.eclipse.microprofile.config.Config config) {
        this(config, null);
    }

    public KafkaConsumer(org.eclipse.microprofile.config.Config config, String consumerGroupId) {
        properties = new KafkaConfigProperties();
        //defaults...
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        properties.setProperty(KafkaConfigProperties.GROUP_ID, getOrGenerateGroupId(consumerGroupId));
        for (String propertyName : config.getPropertyNames()) {
            System.out.printf("propertyName:" + propertyName);
            if(propertyName.startsWith(ConnectorFactory.INCOMING_PREFIX) ) {
                String striped = propertyName.substring(ConnectorFactory.INCOMING_PREFIX.length());
                String channelName = striped.substring(0, striped.indexOf("."));
                String channelPropertyName = striped.substring(channelName.length() + 1);
                System.out.println("KafkaConsumer channelName:" + channelName +
                        " channelPropertyName:" + channelPropertyName +
                        " config.getValue(propertyName, String.class):" + config.getValue(propertyName, String.class));
                if(!channelPropertyName.equals("connector")) properties.setProperty(channelPropertyName,
                        config.getValue(propertyName, String.class));
            }
        }
        this.topicNameList = properties.getTopicNameList();
        this.consumerId = null;
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer(properties);
    }

    public KafkaConsumer(Config config) {
        properties = new KafkaConfigProperties(config);
        properties.setProperty(KafkaConfigProperties.GROUP_ID, getOrGenerateGroupId(null));
        this.topicNameList = properties.getTopicNameList();
        this.consumerId = null;
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer(properties);
    }

    /**
     * Execute supplied consumer for each received record
     *
     * @param function to be executed for each received record
     */
    public Future<?> consumeAsync(Consumer<ConsumerRecord<K, V>> function) {
        return this.consumeAsync(Executors.newWorkStealingPool(), null, function);
    }

    /**
     * Execute supplied consumer by provided executor service for each received record
     *
     * @param executorService Custom executor service used for spinning up polling thread and record consuming threads
     * @param customTopics    Can be null, list of topics appended to the list from configuration
     * @param function        Consumer method executed in new thread for each received record
     * @return The Future's get method will return null when consumer is closed
     */
    public Future<?> consumeAsync(ExecutorService executorService, List<String> customTopics,
                                  Consumer<ConsumerRecord<K, V>> function) {
        LOGGER.info(String.format("Initiating kafka consumer %s listening to topics: %s with groupId: %s",
                consumerId, topicNameList, properties.getProperty(KafkaConfigProperties.GROUP_ID)));

        List<String> mergedTopics = new ArrayList<>();
        mergedTopics.addAll(properties.getTopicNameList());
        mergedTopics.addAll(Optional.ofNullable(customTopics).orElse(Collections.emptyList()));

        if (mergedTopics.isEmpty()) {
            throw new InvalidKafkaConsumerState("No topic names provided in configuration or by parameter.");
        }

        validateConsumer();
        this.executorService = executorService;
        return executorService.submit(() -> {
            consumer.subscribe(mergedTopics, partitionsAssignedLatch);
            try {
                while (!closed.get()) {
                    ConsumerRecords<K, V> consumerRecords = consumer.poll(Duration.ofSeconds(5));
                    consumerRecords.forEach(cr -> executorService.execute(() -> function.accept(cr)));
                }
            } catch (WakeupException ex) {
                if (!closed.get()) {
                    throw ex;
                }
            } finally {
                LOGGER.info("Closing consumer" + consumerId);
                consumer.close();
            }
        });
    }

    public KafkaPublisherBuilder<K, V> createPublisherBuilder(ExecutorService executorService) {
        validateConsumer();
        this.externalExecutorService = executorService;
        System.out.println("KafkaConsumer.createPublisherBuilder");
        return new KafkaPublisherBuilder<>(subscriber -> {
            externalExecutorService.submit(() -> {
                consumer.subscribe(topicNameList, partitionsAssignedLatch);
                try {
                    while (!closed.get()) {
                        System.out.println("KafkaConsumer.createPublisherBuilder !closed.get()");
                        ConsumerRecords<K, V> consumerRecords = consumer.poll(Duration.ofSeconds(5));
                        consumerRecords.forEach(cr -> {
                            KafkaMessage<K, V> kafkaMessage = new KafkaMessage<>(cr);
                            subscriber.onNext(kafkaMessage);
                        });
                    }
                } catch (WakeupException ex) {
                    if (!closed.get()) {
                        throw ex;
                    }
                } finally {
                    LOGGER.info("Closing consumer" + consumerId);
                    consumer.close();
                }
            });
        });
    }

    private void validateConsumer() {
        if (this.closed.get()) {
            throw new InvalidKafkaConsumerState("Invalid consumer state, already closed");
        }
        if (this.executorService != null) {
            throw new InvalidKafkaConsumerState("Invalid consumer state, already consuming");
        }
    }

    /**
     * Blocks current thread until partitions are assigned,
     * since when is consumer effectively ready to receive.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @throws java.lang.InterruptedException        if the current thread is interrupted while waiting
     * @throws java.util.concurrent.TimeoutException if the timeout is reached
     */
    public void waitForPartitionAssigment(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (!partitionsAssignedLatch.await(timeout, unit)) {
            throw new TimeoutException("Timeout for subscription reached");
        }
    }

    /**
     * Close consumer gracefully. Stops polling loop,
     * wakes possible blocked poll and shuts down executor service
     */
    @Override
    public void close() {
        this.closed.set(true);
        this.consumer.wakeup();
        Optional.ofNullable(this.executorService).ifPresent(ExecutorService::shutdown);
    }

    /**
     * Use supplied customGroupId if not null
     * or take it from configuration if exist
     * or generate random in this order
     *
     * @param customGroupId custom group.id, overrides group.id from configuration
     */
    protected String getOrGenerateGroupId(String customGroupId) {
        return Optional.ofNullable(customGroupId)
                .orElse(Optional.ofNullable(properties.getProperty(KafkaConfigProperties.GROUP_ID))
                        .orElse(UUID.randomUUID().toString()));
    }

}
