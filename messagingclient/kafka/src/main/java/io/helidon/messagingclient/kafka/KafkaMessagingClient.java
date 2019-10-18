package io.helidon.messagingclient.kafka;

import io.helidon.messagingclient.*;
import io.helidon.messagingclient.common.InterceptorSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class KafkaMessagingClient implements MessagingClient {
    private final KafkaMessagingClientConfig config;
    private final ExecutorService executorService;
    private final InterceptorSupport interceptors;
    private final MessagingOperations operations;
    private final KafkaProducer<Object, Object> producer;
    final static String TOPIC = "my-example-topic";
    private final static String BOOTSTRAP_SERVERS =
            "localhost:9092";
    private final Consumer<Long, String> consumer;
//            "localhost:9092,localhost:9093,localhost:9094";

    KafkaMessagingClient(KafkaMessagingClientProviderBuilder builder) {
        System.out.println("KafkaMessagingClient.KafkaMessagingClient");
        this.config = builder.messagingConfig();
        this.executorService = builder.executorService();
        System.out.println("KafkaMessagingClient config.username():" + config.username());
        System.out.println("KafkaMessagingClient config.password():" + config.password());
        // todo only create these if configured to do so
        this.producer = initKafkaProducer();
        this.consumer = initKakfaConsumer();
        this.interceptors = builder.interceptors();
        this.operations = builder.operations();
    }

    /**
     * Constructor helper to build KafkaProducer from provided configuration.
     */
    private KafkaProducer<Object, Object> initKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,  BOOTSTRAP_SERVERS);
        //todo paul get from config and allow config to specify server.properties , consumer.properties
        // , producer.properties (where bootstrap.servers=localhost:9092 is specified), etc.
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        return new KafkaProducer<Object, Object>(props);
    }

    // http://cloudurable.com/blog/kafka-tutorial-kafka-consumer/index.html
    private Consumer<Long, String> initKakfaConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,   BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        final Consumer<Long, String> consumer = new KafkaConsumer<>(props);
        return consumer;
    }

    @Override
    public CompletionStage<Void> ping() {
        return null;
    }

    @Override
    public String messagingType() {
        return "kafka";
    }

    @Override
    public <T> T listenForMessages(Function<MessagingListenForMessages, T> executor) {
        return executor.apply(
                new KafkaExecute(
                        executorService, operations, interceptors, producer, consumer));
    }

    @Override
    public void sendMessages(Object o) {
        System.out.println("KafkaMessagingClient.sendMessages");
        long time = System.currentTimeMillis();
        int sendMessageCount = 1;
        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Object, Object> record =
                        new ProducerRecord<>(TOPIC, index,  o.toString() + index);
                RecordMetadata metadata = producer.send(record).get();
                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("KafkaMessagingClient.sendMessages sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
//            producer.flush();
//            producer.close();
        }
    }

    private class KafkaExecute extends AbstractMessagingExecute implements MessagingListenForMessages {
        private final ExecutorService executorService;
        private final InterceptorSupport interceptors;
        private final KafkaProducer<Object, Object> producer;
        private final Consumer<Long, String> consumer;

        KafkaExecute(ExecutorService executorService,
                    MessagingOperations operations,
                    InterceptorSupport interceptors,
                     KafkaProducer<Object, Object> producer,
                     Consumer<Long, String> consumer) {
            super(operations);
            this.executorService = executorService;
            this.interceptors = interceptors;
            this.producer = producer;
            this.consumer = consumer;
        }

        //todo paul wrong should pass pool or some such for publisher and consumer to be created from
        @Override
        public <D extends MessagingOperation<D, R>, R> MessagingOperation<D, R> createNamedFilter(String insert2) {
            return new KafkaMessagingOperation(executorService, interceptors, producer, consumer);
        }
    }
}
