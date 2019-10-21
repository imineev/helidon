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
import org.apache.kafka.common.serialization.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

//todo this is simply Producer/Consumer API with executor and so add (likely in different operation class)...
// - Reactor Kafka
// - Connector API
// - Streams API
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

    public CompletionStage<Message> incoming(MessageProcessor messageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.operation incoming"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        Disposable disposable;
        ReceiverOptions<Integer, String> receiverOptions;
        SimpleDateFormat dateFormat;
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapservers());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "sample-consumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        receiverOptions = ReceiverOptions.create(props);
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");

        ReceiverOptions<Integer, String> options = receiverOptions.subscription(Collections.singleton(config.topic()))
                .addAssignListener(partitions -> System.out.println("onPartitionsAssigned {}:" + partitions))
                .addRevokeListener(partitions -> System.out.println("onPartitionsRevoked {}:" + partitions));
        Flux<ReceiverRecord<Integer, String>> kafkaFlux = KafkaReceiver.create(options).receive();
        disposable = kafkaFlux.subscribe(record -> {
            ReceiverOffset offset = record.receiverOffset();
            System.out.printf("Received message: topic-partition=%s offset=%d timestamp=%s key=%d value=%s\n",
                    offset.topicPartition(),
                    offset.offset(),
                    dateFormat.format(new Date(record.timestamp())),
                    record.key(),
                    record.value());
            Message message = new KafkaMessage(record.value());
            messageProcessor.processMessage(message);
            operationFuture.complete(null);
            queryFuture.complete(message);
            offset.acknowledge();
            latch.countDown();
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        disposable.dispose();
        return queryFuture;
    }


    public CompletionStage<Message> incomingExecutor(MessageProcessor messageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.operation incoming"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doIncoming(messagingContextFuture, operationFuture, queryFuture, messageProcessor);
    }

    protected CompletionStage<Message> doIncoming(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                  CompletableFuture<Void> operationFuture,
                                                  CompletableFuture<Message> queryFuture,
                                                  MessageProcessor messageProcessor) {
        System.out.println("KafkaMessagingOperation.doIncoming");
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
                        if (consumerRecords.count() == 0) {
                            continue;
                        }
                        consumerRecords.forEach(record -> {
                            String messageTxt = "Consumer Record:" + record.key() + "," + record.value() + "," +
                                    record.partition() + "," + record.offset();
                            System.out.println("KafkaMessagingOperation.doIncoming messageTxt:" + messageTxt);
                            operationFuture.complete(null);
                            KafkaMessage message = new KafkaMessage(messageTxt);
                            messageProcessor.processMessage(message);
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

    public CompletionStage<Message> outgoing(MessageProcessor messageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.operation outgoing"));

        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        KafkaSender<Integer, String> sender;
        SimpleDateFormat dateFormat;
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapservers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);

        sender = KafkaSender.create(senderOptions);
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");
        sender.<Integer>send(Flux.range(1, count)
                .map(i -> SenderRecord.create(new ProducerRecord<>(config.topic(), i, "Message_" + i), i)))
                .doOnError(e -> System.out.println("Send failed:" + e))
                .subscribe(r -> {
                    RecordMetadata metadata = r.recordMetadata();
                    System.out.printf("Message %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s\n",
                            r.correlationMetadata(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            dateFormat.format(new Date(metadata.timestamp())));
                    operationFuture.complete(null);
                    KafkaMessage message = new KafkaMessage(metadata.toString());
                    messageProcessor.processMessage(message);
                    queryFuture.complete(message);
                    latch.countDown();
                });
        return queryFuture;
    }

    public CompletionStage<Message> outgoingExecutor(MessageProcessor messageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.operation outgoing"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doOutgoing(messagingContextFuture, operationFuture, queryFuture, messageProcessor);
    }

    protected CompletionStage<Message> doOutgoing(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                  CompletableFuture<Void> operationFuture,
                                                  CompletableFuture<Message> queryFuture,
                                                  MessageProcessor messageProcessor) {
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
                                new ProducerRecord<>(config.topic(), index, "test message");
                        RecordMetadata metadata = producer.send(record).get();
                        long elapsedTime = System.currentTimeMillis() - time;
                        System.out.printf("KafkaMessagingClient.sendMessages sent record(key=%s value=%s) " +
                                        "meta(partition=%d, offset=%d) time=%d\n",
                                record.key(), record.value(), metadata.partition(),
                                metadata.offset(), elapsedTime);
                        operationFuture.complete(null);
                        KafkaMessage message = new KafkaMessage("test message");
                        messageProcessor.processMessage(message);
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

    private KafkaProducer<Object, Object> initKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapservers());
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
