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

//todo this is simply Producer/Consumer API with executor and so add (likely in different channel class)...
// - Reactor Kafka
// - Connector API
// - Streams API
public class KafkaMessagingChannel implements MessagingChannel {
    private final ExecutorService executorService;
    private final KafkaMessagingClientConfig config;
    private final InterceptorSupport interceptors;
    private final String messagingType = "kafka";
    private final String channelName = "all"; //todo should serve the filter(name) purpose, verify messagecontext (update)
    private final String filter;
    private final MessagingChannelType channelType = MessagingChannelType.MESSAGING;

    private static final Logger LOGGER = Logger.getLogger(KafkaMessagingChannel.class.getName());

    public KafkaMessagingChannel(ExecutorService executorService,
                                 InterceptorSupport interceptors,
                                 KafkaMessagingClientConfig config, String filter) {
        this.executorService = executorService;
        this.interceptors = interceptors;
        this.config = config;
        this.filter = filter;
    }

    public CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor) {
        return incoming(messageProcessor, false);
    }

    public CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor, boolean isCloseSession) {
        LOGGER.fine(() -> String.format("KafkaMessagingChannel.channel incoming"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        messagingContextFuture.exceptionally(throwable -> {
            channelFuture.completeExceptionally(throwable);
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
        LOGGER.fine(() -> String.format("KafkaMessagingChannel.channel incoming kafkaFlux:" + kafkaFlux));
        disposable = kafkaFlux.subscribe(record -> {
            ReceiverOffset offset = record.receiverOffset();
            System.out.printf("incoming received message: topic-partition=%s offset=%d timestamp=%s key=%d value=%s\n",
                    offset.topicPartition(),
                    offset.offset(),
                    dateFormat.format(new Date(record.timestamp())),
                    record.key(),
                    record.value());
            System.out.println("KafkaMessagingChannel.incoming record.value():"+record.value());
            HelidonMessage message = new KafkaMessage(record.value());
            messageProcessor.processMessage(message);
            channelFuture.complete(null);
            queryFuture.complete(message);
            offset.acknowledge();
            latch.countDown();
        });
        LOGGER.fine(() -> String.format("KafkaMessagingChannel.channel incoming disposable:" + disposable));
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        disposable.dispose();
        return queryFuture;
    }


    @Override
    public CompletionStage<HelidonMessage> outgoing(
            MessageProcessor messageProcessor, HelidonMessage message) {
        return outgoing(messageProcessor, message, null, null);
    }

    @Override
    public CompletionStage<HelidonMessage> outgoing(
            MessageProcessor messageProcessor, HelidonMessage kafkamessage, Object session, String queueName) {
        System.out.println("KafkaMessagingChannel.channel outgoing");
        LOGGER.fine(() -> String.format("KafkaMessagingChannel.channel outgoing"));

        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        messagingContextFuture.exceptionally(throwable -> {
            channelFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        int count = 1;
        CountDownLatch latch = new CountDownLatch(count);
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapservers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);
        KafkaSender<Integer, String> sender = KafkaSender.create(senderOptions);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");
        System.out.println("KafkaMessagingChannel.channel outgoing count:" + count);
        sender.<Integer>send(Flux.range(1, count)
                .map(i -> SenderRecord.create(new ProducerRecord<>(config.topic(), i, kafkamessage.getString() + i), i)))
                .doOnError(e -> System.out.println("Send failed:" + e))
                .subscribe(r -> {
                    RecordMetadata metadata = r.recordMetadata();
                    System.out.printf("HelidonMessage %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s\n",
                            r.correlationMetadata(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            dateFormat.format(new Date(metadata.timestamp())));
                    channelFuture.complete(null);
                    KafkaMessage message = new KafkaMessage(metadata.toString());
                    messageProcessor.processMessage(message);
                    queryFuture.complete(message); //todo returns metadata message currently
                    latch.countDown();
                });
        return queryFuture;
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

        for (MessagingInterceptor interceptor : interceptors.interceptors(channelType(), channelName())) {
            result = result.thenCompose(interceptor::channel);
        }

        return result;
    }

    private MessagingChannelType channelType() {
        return channelType;
    }

    private String channelName() {
        return channelName;
    }


    /**
     * Update the interceptor context with the channel name, channel and
     * channel parameters.
     *
     * @param messagingContext interceptor context
     */
    protected void update(MessagingInterceptorContext messagingContext) {
        messagingContext.channelName(channelName);
//        initParameters(ParamType.INDEXED);
//
//        if (paramType == ParamType.NAMED) {
//            messagingContext.channel(channel, parameters.namedParams());
//        } else {
//            messagingContext.channel(channel, parameters.indexedParams());
//        }
//        messagingContext.channelType(channelType());
    }


    protected String messagingType() {
        return messagingType;
    }






// todo unused delete...



    public CompletionStage<HelidonMessage> incomingExecutor(MessageProcessor messageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingChannel.channel incoming"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doIncomingExecutor(messagingContextFuture, channelFuture, queryFuture, messageProcessor);
    }

    protected CompletionStage<HelidonMessage> doIncomingExecutor(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                                 CompletableFuture<Void> channelFuture,
                                                                 CompletableFuture<HelidonMessage> queryFuture,
                                                                 MessageProcessor messageProcessor) {
        System.out.println("KafkaMessagingChannel.doIncoming");
        messagingContextFuture.exceptionally(throwable -> {
            channelFuture.completeExceptionally(throwable);
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
                            System.out.println("KafkaMessagingChannel.doIncoming messageTxt:" + messageTxt);
                            channelFuture.complete(null);
                            KafkaMessage message = new KafkaMessage(messageTxt);
                            messageProcessor.processMessage(message);
                            queryFuture.complete(message);
                        });
                        consumer.commitAsync();
                    }
                } catch (Exception e) {
                    channelFuture.completeExceptionally(e);
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

    public CompletionStage<HelidonMessage> outgoingExecutor(MessageProcessor messageProcessor) {
        LOGGER.fine(() -> String.format("KafkaMessagingChannel.channel outgoing"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doOutgoingExecutor(messagingContextFuture, channelFuture, queryFuture, messageProcessor);
    }

    protected CompletionStage<HelidonMessage> doOutgoingExecutor(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                                 CompletableFuture<Void> channelFuture,
                                                                 CompletableFuture<HelidonMessage> queryFuture,
                                                                 MessageProcessor messageProcessor) {
        System.out.println("KafkaMessagingChannel.doOutgoing");
        // query and channel future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            channelFuture.completeExceptionally(throwable);
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
                        channelFuture.complete(null);
                        KafkaMessage message = new KafkaMessage("test message");
                        messageProcessor.processMessage(message);
                        queryFuture.complete(message);
                    }
                } catch (Exception e) {
                    channelFuture.completeExceptionally(e);
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
}
