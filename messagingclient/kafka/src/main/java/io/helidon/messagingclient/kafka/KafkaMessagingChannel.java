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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
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

    //Outgoing methods (8) are called as follows ...
    //once at assembly time if...
    // Publisher<Message<O>> method() - Returns a stream of Message
    // Publisher<O> method() - Returns a stream of payload of type O, payloads are mapped to Message<O> by impl
    // PublisherBuilder<Message<O>> method() - Returns a stream of Message
    //once at subscription time if...
    // PublisherBuilder<O> method() - Returns a stream of payload, payloads are mapped to Message<O> by impl
    //for each request made by the subscriber if...
    // Message<O> method() - Produces an infinite stream of Message
    // O method() - Produces an infinite stream of payload, payloads are mapped to Message<O> by impl
    // CompletionStage<Message<O>> method() - Produces an infinite stream of Message, not called by impl until the CompletionStage returned previously is completed
    // CompletionStage<O> method() - roduces an infinite stream of payload, payloads are mapped to Message<O> by impl, not called by impl until the CompletionStage returned previously is completed

    //Incoming methods (7) are called as follows...
    //once at assembly time (to retrieve Subscriber which is then connected to the matching channel) ...
    // Subscriber<Message<I>> method() - Returns a Subscriber that receives the Message objects transiting on the channel
    // Subscriber<I> method() - Returns a Subscriber that receives the payload objects transiting on the channel, extracted using using Message.getPayload()
    //once at assembly time (to retrieve SubscriberBuilder that is used to build a CompletionSubscriber that is subscribed to the matching channel) ...
    // SubscriberBuilder<Message<I>> method() - Returns a SubscriberBuilder that receives the Message objects transiting on the channel
    // SubscriberBuilder<I> method() - Returns a SubscriberBuilder that is used to build aCompletionSubscriber<I >` that receives the payload, extracted using using Message.getPayload()
    //called for every Message<I> instance transiting on the channel (not called concurrently so must return before being called with the next payload) ...
    // void method(I payload) - Consumes the payload
    //called for every Message<I> instance transiting on the channel (must wait until the completion of the previously returned CompletionStage before calling the method again )...
    // CompletionStage<?> method(Message<I> msg) - Consumes the Message
    //called for every Message<I> instance transiting on the channel, payload extracted from the inflight messages (must wait until the completion of the previously returned CompletionStage before calling the method again )...
    // CompletionStage<?> method(I payload) - Consumes the payload asynchronously

    //IncomingOutgoing (ie Processor) (16) methods are called as follows...
    //once at assembly time ...
    // Processor<Message<I>, Message<O>> method() - Returns a Reactive Streams processor consuming incoming Message instances and produces Message instances
    // Processor<I, O> method();
    // ProcessorBuilder<Message<I>, Message<O>> method();
    // ProcessorBuilder<I, O> method();
    //called for every incoming message. Implementations must not call the method subsequently until the stream from the previously returned Publisher is completed. ...
    // Publisher<Message<O>> method(Message<I> msg)
    // Publisher<O> method(I payload)
    //same but return PublisherBuilder
    // PublisherBuilder<Message<O>> method (Message<I> msg)
    // PublisherBuilder<O> method(I payload)
    //called for every incoming message. Implementations must not call the method subsequently until the previous call must have returned.
    // Message<O> method(Message<I> msg)
    // O method(I payload)
    //...
    // CompletionStage<Message<O>> method (Message<I> msg)
    //...
    // CompletionStage<O> method(I payload)
    //once at assembly time...
    // Publisher<Message<O>> method(Publisher<Message<I>> pub)
    // PublisherBuilder<Message<O>> method(PublisherBuilder<Message<I>> pub)
    // Publisher<O> method(Publisher<I> pub)
    // PublisherBuilder<O> method(PublisherBuilder<I> pub)

    //Acknowledgments ...
    // • NONE - no acknowledgment is performed
    // • MANUAL - the user is responsible for the acknowledgement, by calling the Message#ack() method,
    //            so the Reactive Messaging implementation does not apply implicit acknowledgement
    // • PRE_PROCESSING - the Reactive Messaging implementation acknowledges the message before the annotated method or processing is executed
    // • POST_PROCESSING - the Reactive Messaging implementation acknowledges the message once:
    //          1. the method or processing completes if the method does not emit data
    //          2. when the emitted data is acknowledged

    //    @Outgoing("data")
    public Publisher<Integer> source() {
        SubmissionPublisher submissionPublisher =  new SubmissionPublisher();
        return (Publisher) subscriber -> submissionPublisher.subscribe(new Flow.Subscriber() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        subscription.request(n);
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onNext(Object item) {
                subscriber.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
    }


    public CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor) {
        try {
//
            Method[] methods = messageProcessor.getClass().getMethods();
            methods[0].getReturnType();
            //continue to process..
            // eg if "void method(I payload)"
            // called for every Message<I> instance transiting on the channel
            // (not called concurrently so must return before being called with the next payload) ...
            // subscribe and call for every message...
            // messageProcessor.getClass().getMethod("someincomingmethod",String.class).invoke(
            ////                    messageProcessor, null);
//        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        //see abstractmediator
        return incoming(messageProcessor, false);
    }

    public CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor, boolean isCloseSession) {
       return incomingExecutor(messageProcessor);
    }


    @Override
    public CompletionStage<HelidonMessage> outgoing(
            MessageProcessor messageProcessor, HelidonMessage message) {
        return outgoingExecutor(messageProcessor);
    }

    @Override
    public CompletionStage<HelidonMessage> outgoing(MessageProcessor messageProcessor, HelidonMessage message, Object session, String queueName) {
        return outgoingExecutor(messageProcessor);
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
