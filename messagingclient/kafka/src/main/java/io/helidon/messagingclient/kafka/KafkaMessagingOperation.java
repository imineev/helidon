package io.helidon.messagingclient.kafka;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.messagingclient.*;
import io.helidon.messagingclient.common.InterceptorSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class KafkaMessagingOperation implements MessagingOperation {
    private final ExecutorService executorService;
    private final KafkaProducer<Object, Object> producer;
    private final Consumer<Long, String> consumer;
    private final InterceptorSupport interceptors;
    private final String messagingType = "kafka";
    private final String operationName = "all"; //todo
    private final MessagingOperationType operationType = MessagingOperationType.MESSAGING;

    private static final Logger LOGGER = Logger.getLogger(KafkaMessagingOperation.class.getName());

    public KafkaMessagingOperation(ExecutorService executorService,
                                   InterceptorSupport interceptors, KafkaProducer<Object, Object> producer,
                                   Consumer<Long, String> consumer) {
        this.executorService = executorService;
        this.producer = producer;
        this.consumer = consumer;
        this.interceptors = interceptors;
    }

    @Override
    public Object namedParam(Object parameters) {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.namedParam parameters: %s", parameters));
        return null;
    }

    public CompletionStage<Message> execute() {
        LOGGER.fine(() -> String.format("KafkaMessagingOperation.listenForMessages"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> dbContextFuture = invokeInterceptors(messagingContext);
        return doExecute(dbContextFuture, operationFuture, queryFuture);
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
     * @param dbContext interceptor context
     */
    protected void update(MessagingInterceptorContext dbContext) {
        dbContext.operationName(operationName);
//        initParameters(ParamType.INDEXED);
//
//        if (paramType == ParamType.NAMED) {
//            dbContext.operation(operation, parameters.namedParams());
//        } else {
//            dbContext.operation(operation, parameters.indexedParams());
//        }
//        dbContext.operationType(operationType());
    }


    protected String messagingType() {
        return messagingType;
    }

    protected CompletionStage<Message> doExecute(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                 CompletableFuture<Void> operationFuture,
                                                 CompletableFuture<Message> queryFuture) {
        System.out.println("KafkaMessagingOperation.doExecute");
        // query and operation future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        return messagingContextFuture.thenCompose(dbContext -> {
            executorService.submit(() -> {
                try (Consumer<Long, String> consumer = this.consumer) {
                    //todo paul should actually be config instead of consumer passed in and create consumer here..
                    // also should get topic from config
                    consumer.subscribe(Collections.singletonList(KafkaMessagingClient.TOPIC));
                    int messageCountFromConfig = 1; //todo make configurable
                    int messageCount = 0;
                    while (messageCount < messageCountFromConfig) {
                        final ConsumerRecords<Long, String> consumerRecords =
                                consumer.poll(2000);  //deprecated use Duration param
                        if (consumerRecords.count()==0) {
                            continue;
                        }
                        consumerRecords.forEach(record -> {
                            String messageTxt = "Consumer Record:" + record.key() +"," + record.value() + "," +
                                    record.partition()+ "," + record.offset();
                            System.out.println("KafkaMessagingOperation.doExecute messageTxt:" + messageTxt);
                            operationFuture.complete(null);
                            queryFuture.complete(new KafkaMessage(messageTxt, new Object()));
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
}
