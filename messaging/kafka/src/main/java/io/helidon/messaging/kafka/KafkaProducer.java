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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Simple Kafka producer covering basic use-cases.
 * Configurable by Helidon {@link io.helidon.config.Config Config},
 * For more info about configuration see {@link KafkaConfigProperties}
 * <p/>
 * Usage:
 * <pre>{@code new KafkaProducer<Long, String>("job-done-producer", Config.create())
 *             .produce("Hello world!");
 * }</pre>
 *
 * @param <K> Key type
 * @param <V> Value type
 * @see KafkaConfigProperties
 * @see io.helidon.config.Config
 */
public class KafkaProducer<K, V> implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(KafkaProducer.class.getName());
    private final KafkaConfigProperties properties;

    private org.apache.kafka.clients.producer.KafkaProducer producer;
    private ExecutorService externalExecutorService;

    /**
     * Kafka producer created from {@link io.helidon.config.Config config} under kafka->producerId,
     * see configuration {@link KafkaConfigProperties example}.
     *
     * @param producerId key in configuration
     * @param config     Helidon {@link io.helidon.config.Config config}
     * @see KafkaConfigProperties
     * @see io.helidon.config.Config
     */
    public KafkaProducer(String producerId, Config config) {
        properties = new KafkaConfigProperties(config.get("mp.messaging.outcoming").get(producerId));
        producer = new org.apache.kafka.clients.producer.KafkaProducer(properties);
    }

    public KafkaProducer(Config config) {
        properties = new KafkaConfigProperties(config);
        producer = new org.apache.kafka.clients.producer.KafkaProducer(properties);
    }


    public KafkaProducer(org.eclipse.microprofile.config.Config config) {
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
        properties.setProperty(KafkaConfigProperties.GROUP_ID, getOrGenerateGroupId(null));
        for (String propertyName : config.getPropertyNames()) {
            System.out.printf("propertyName:" + propertyName);
            if(propertyName.startsWith(ConnectorFactory.OUTGOING_PREFIX) ) {
                String striped = propertyName.substring(ConnectorFactory.OUTGOING_PREFIX.length());
                String channelName = striped.substring(0, striped.indexOf("."));
                String channelPropertyName = striped.substring(channelName.length() + 1);
                System.out.println("KafkaProducer channelName:" + channelName +
                        " channelPropertyName:" + channelPropertyName +
                        " config.getValue(propertyName, String.class):" + config.getValue(propertyName, String.class));
                if(!channelPropertyName.equals("connector")) properties.setProperty(channelPropertyName,
                        config.getValue(propertyName, String.class));
            }
        }
        producer = new org.apache.kafka.clients.producer.KafkaProducer(properties);
    }

    protected String getOrGenerateGroupId(String customGroupId) {
        return Optional.ofNullable(customGroupId)
                .orElse(Optional.ofNullable(properties.getProperty(KafkaConfigProperties.GROUP_ID))
                        .orElse(UUID.randomUUID().toString()));
    }

    /**
     * Send record to all provided topics,
     * blocking until all records are acknowledged by broker
     *
     * @param value Will be serialized by <b>value.serializer</b> class
     *              defined in {@link KafkaConfigProperties configuration}
     * @return Server acknowledged metadata about sent topics
     */
    public List<RecordMetadata> produce(V value) {
        List<Future<RecordMetadata>> futureRecords =
                this.produceAsync(null, null, null, null, value, null);
        List<RecordMetadata> metadataList = new ArrayList<>(futureRecords.size());

        for (Future<RecordMetadata> future : futureRecords) {
            try {
                metadataList.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to send topic", e);
            }
        }
        return metadataList;
    }

    public List<Future<RecordMetadata>> produceAsync(V value) {
        return this.produceAsync(null, null, null, null, value, null);
    }

    /**
     * Send record to all provided topics, don't wait for server acknowledgement
     *
     * @param customTopics Can be null, list of topics appended to the list from configuration,
     *                     record will be sent to all topics iteratively
     * @param partition    Can be null, if key is also null topic is sent to random partition
     * @param timestamp    Can be null System.currentTimeMillis() is used then
     * @param key          Can be null, if not, topics are grouped to partitions by key
     * @param value        Will be serialized by value.serializer class defined in configuration
     * @param headers      Can be null, custom headers for additional meta information if needed
     * @return Futures of server acknowledged metadata about sent topics
     */
    public List<Future<RecordMetadata>> produceAsync(List<String> customTopics,
                                                     Integer partition,
                                                     Long timestamp,
                                                     K key,
                                                     V value,
                                                     Iterable<Header> headers) {

        List<String> mergedTopics = new ArrayList<>();
        mergedTopics.addAll(properties.getTopicNameList());
        mergedTopics.addAll(Optional.ofNullable(customTopics).orElse(Collections.emptyList()));

        if (mergedTopics.isEmpty()) {
            LOGGER.warning("No topic names provided in configuration or by parameter. Nothing sent.");
            return Collections.emptyList();
        }

        List<Future<RecordMetadata>> recordMetadataFutures = new ArrayList<>(mergedTopics.size());

        for (String topic : mergedTopics) {
            ProducerRecord<K, V> record = new ProducerRecord<>(topic, partition, timestamp, key, value, headers);
            LOGGER.fine(String.format("Sending topic: %s to partition %d", topic, partition));
            recordMetadataFutures.add(producer.send(record));
        }
        return recordMetadataFutures;
    }

    @Override
    public void close() {
        producer.close();
    }


    public KafkaSubscriberBuilder<K, V> createSubscriberBuilder(V executorService) {
//        return new KafkaSubscriberBuilder;

//        this.externalExecutorService = executorService;
        System.out.println("KafkaProducer.createSubscriberBuilder");
        //todo this is incorrect/incomplete, would be sent via SubscriberBuilder/CompletionSubscriber using OutgoingMessagingService etc
        ProducerRecord<K, V> kvProducerRecord = new ProducerRecord((String)properties.get("topic"), "test");
        producer.send(kvProducerRecord);
        return new KafkaSubscriberBuilder(); //todo...

//        return new KafkaSubscriberBuilder<K, V>(subscriber -> {
//            externalExecutorService.submit(subscriber -> {
//                producer.send(null);
//                consumer.subscribe(topicNameList, partitionsAssignedLatch);
//                try {
//                    while (!closed.get()) {
//                        System.out.println("KafkaConsumer.createPublisherBuilder !closed.get()");
//                        ConsumerRecords<K, V> consumerRecords = consumer.poll(Duration.ofSeconds(5));
//                        consumerRecords.forEach(cr -> {
//                            KafkaMessage<K, V> kafkaMessage = new KafkaMessage<>(cr);
//                            subscriber.onNext(kafkaMessage);
//                        });
//                    }
//                } catch (WakeupException ex) {
//                    if (!closed.get()) {
//                        throw ex;
//                    }
//                } finally {
//                    LOGGER.info("Closing consumer" + consumerId);
//                    consumer.close();
//                }
//            });
//        });
        /**
        return new KafkaSubscriberBuilder<K, V>(subscriber -> {
            externalExecutorService.submit(() -> {

                ProducerRecord<K, V> kvProducerRecord;
                kvProducerRecord = new ProducerRecord<K, V>("demotopic", null);
                producer.send(kvProducerRecord);
//                        consumer.subscribe(topicNameList, partitionsAssignedLatch);
                try {
//                    while (!closed.get()) {
//                        System.out.println("KafkaConsumer.createPublisherBuilder !closed.get()");
//                        ConsumerRecords<K, V> consumerRecords = consumer.poll(Duration.ofSeconds(5));
//                        consumerRecords.forEach(cr -> {
//                            KafkaMessage<K, V> kafkaMessage = new KafkaMessage<>(cr);
//                            subscriber.onNext(kafkaMessage);
//                        });
//                    }
                } catch (WakeupException ex) {
//                    if (!closed.get()) {
//                        throw ex;
//                    }
                } finally {
                    LOGGER.info("Closing producer" + producer);
                    producer.close();
                }
            });
        });
         */
    }
}
