package io.helidon.messagingclient.kafka;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.messagingclient.*;
import io.helidon.messagingclient.common.InterceptorSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class KafkaMessagingOperation implements MessagingOperation {
    private final ExecutorService executorService;
    private final KafkaMessagingClientConfig config;
    private final InterceptorSupport interceptors;
    private final String messagingType = "kafka";
    private final String operationName = "all"; //todo should serve the filter(name) purpose, verify messagecontext (update)
    private final String filter;
    private final MessagingOperationType operationType = MessagingOperationType.MESSAGING;

    private static final Logger LOGGER = Logger.getLogger(KafkaMessagingOperation.class.getName());

    public KafkaMessagingOperation(ExecutorService executorService,
                                   InterceptorSupport interceptors,
                                   KafkaMessagingClientConfig config, String filter) {
        this.executorService = executorService;
        this.interceptors = interceptors;
        this.config = config;
        this.filter = filter;
    }

    public CompletionStage<Message> incoming(MessageProcessor testMessageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.operation incoming"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doIncoming(messagingContextFuture, operationFuture, queryFuture, testMessageProcessor);
    }

    protected CompletionStage<Message> doIncoming(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                  CompletableFuture<Void> operationFuture,
                                                  CompletableFuture<Message> queryFuture,
                                                  MessageProcessor testMessageProcessor) {
        System.out.println("KafkaMessagingOperation.doIncoming");
        // query and operation future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        Consumer<Long, String> kafkaConsumer = initKakfaConsumer();
        return messagingContextFuture.thenCompose(messagingContext -> {
            executorService.submit(() -> {
                try (Consumer<Long, String> consumer = kafkaConsumer) {
                    consumer.subscribe(Collections.singletonList(config.topic()));
                    while (true) {
                        final ConsumerRecords<Long, String> consumerRecords =
                                consumer.poll(1000);  //deprecated use Duration param
                        if (consumerRecords.count()==0) {
                            continue;
                        }
                        consumerRecords.forEach(record -> {
                            String messageTxt = "Consumer Record:" + record.key() +"," + record.value() + "," +
                                    record.partition()+ "," + record.offset();
                            System.out.println("KafkaMessagingOperation.doIncoming messageTxt:" + messageTxt);
                            operationFuture.complete(null);
                            KafkaMessage message = new KafkaMessage(messageTxt);
                            testMessageProcessor.processMessage(message);
                            queryFuture.complete(message);
                        });
                        consumer.commitAsync();
                    }
                } catch (Exception e) {
                    operationFuture.completeExceptionally(e);
                    queryFuture.completeExceptionally(e);
                }
            });
            return queryFuture;
        });
    }

    private Consumer<Long, String> initKakfaConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapservers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        final Consumer<Long, String> consumer = new KafkaConsumer<>(props);
        return consumer;
    }

    public CompletionStage<Message> outgoing(MessageProcessor testMessageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.operation outgoing"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doOutgoing(messagingContextFuture, operationFuture, queryFuture, testMessageProcessor);
    }

    protected CompletionStage<Message> doOutgoing(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                  CompletableFuture<Void> operationFuture,
                                                  CompletableFuture<Message> queryFuture,
                                                  MessageProcessor testMessageProcessor) {
        System.out.println("KafkaMessagingOperation.doOutgoing");
        // query and operation future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        KafkaProducer<Object, Object> producer = initKafkaProducer();
        return messagingContextFuture.thenCompose(messagingContext -> {
            executorService.submit(() -> {



                System.out.println("KafkaMessagingClient.sendMessages");
                long time = System.currentTimeMillis();
                int sendMessageCount = 1;
                try {
                    for (long index = time; index < time + sendMessageCount; index++) {
                        final ProducerRecord<Object, Object> record =
                                new ProducerRecord<>(config.topic(), index,  "test message");
                        RecordMetadata metadata = producer.send(record).get();
                        long elapsedTime = System.currentTimeMillis() - time;
                        System.out.printf("KafkaMessagingClient.sendMessages sent record(key=%s value=%s) " +
                                        "meta(partition=%d, offset=%d) time=%d\n",
                                record.key(), record.value(), metadata.partition(),
                                metadata.offset(), elapsedTime);
                        operationFuture.complete(null);
                        KafkaMessage message = new KafkaMessage("test message");
                        testMessageProcessor.processMessage(message);
                        queryFuture.complete(message);
                    }
                } catch (Exception e) {
                    operationFuture.completeExceptionally(e);
                    queryFuture.completeExceptionally(e);
                } finally { // only if not -1 ...
//            producer.flush();
//            producer.close();
                }
            });
            return queryFuture;
        });
    }

//                try (Consumer<Long, String> consumer = kafkaConsumer) {
//                    consumer.subscribe(Collections.singletonList(config.topic()));
//                    while (true) {
//                        final ConsumerRecords<Long, String> consumerRecords =
//                                consumer.poll(1000);  //deprecated use Duration param
//                        if (consumerRecords.count()==0) {
//                            continue;
//                        }
//                        consumerRecords.forEach(record -> {
//                            String messageTxt = "Consumer Record:" + record.key() +"," + record.value() + "," +
//                                    record.partition()+ "," + record.offset();
//                            System.out.println("KafkaMessagingOperation.doIncoming messageTxt:" + messageTxt);
//                            operationFuture.complete(null);
//                            KafkaMessage message = new KafkaMessage(messageTxt);
//                            testMessageProcessor.processMessage(message);
//                            queryFuture.complete(message);
//                        });
//                        consumer.commitAsync();
//                    }
//                } catch (Exception e) {
//                    operationFuture.completeExceptionally(e);
//                    queryFuture.completeExceptionally(e);
//                }

    private KafkaProducer<Object, Object> initKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,  config.bootstrapservers());
        //todo paul get from config and allow config to specify server.properties , consumer.properties
        // , producer.properties (where bootstrap.servers=localhost:9092 is specified), etc.
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    /**
     * Invoke all interceptors.
     *
     * @param messagingContext initial interceptor context
     * @return future with the result of interceptors processing
     */
    CompletionStage<MessagingInterceptorContext> invokeInterceptors(MessagingInterceptorContext messagingContext) {
        CompletableFuture<MessagingInterceptorContext> result = CompletableFuture.completedFuture(messagingContext);

        messagingContext.context(Contexts.context().orElseGet(Context::create));

        for (MessagingInterceptor interceptor : interceptors.interceptors(operationType(), operationName())) {
            result = result.thenCompose(interceptor::operation);
        }

        return result;
    }

    private MessagingOperationType operationType() {
        return operationType;
    }

    private String operationName() {
        return operationName;
    }


    /**
     * Update the interceptor context with the operation name, operation and
     * operation parameters.
     *
     * @param messagingContext interceptor context
     */
    protected void update(MessagingInterceptorContext messagingContext) {
        messagingContext.operationName(operationName);
//        initParameters(ParamType.INDEXED);
//
//        if (paramType == ParamType.NAMED) {
//            messagingContext.operation(operation, parameters.namedParams());
//        } else {
//            messagingContext.operation(operation, parameters.indexedParams());
//        }
//        messagingContext.operationType(operationType());
    }


    protected String messagingType() {
        return messagingType;
    }

}
