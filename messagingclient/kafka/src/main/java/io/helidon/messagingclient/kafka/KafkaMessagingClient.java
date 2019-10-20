package io.helidon.messagingclient.kafka;

import io.helidon.messagingclient.*;
import io.helidon.messagingclient.common.InterceptorSupport;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class KafkaMessagingClient implements MessagingClient {
    private final KafkaMessagingClientConfig config;
    private final ExecutorService executorService;
    private final InterceptorSupport interceptors;
    private final MessagingOperations operations;

    KafkaMessagingClient(KafkaMessagingClientProviderBuilder builder) {
        this.config = builder.messagingConfig();
        this.executorService = builder.executorService();
        System.out.println("KafkaMessagingClient config.topic():" + config.topic());
        System.out.println("KafkaMessagingClient config.bootstrapservers():" + config.bootstrapservers());
        System.out.println("KafkaMessagingClient config.numberofmessagestoconsume():" + config.numberofmessagestoconsume());
        this.interceptors = builder.interceptors();
        this.operations = builder.operations();
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
    public <T> T operation(Function<MessagingOperationOptions, T> executor) {
        return executor.apply(
                new KafkaExecute(executorService, operations, interceptors, config));
    }

    private class KafkaExecute extends AbstractMessagingExecute implements MessagingOperationOptions {
        private final ExecutorService executorService;
        private final InterceptorSupport interceptors;
        private final KafkaMessagingClientConfig config;

        KafkaExecute(ExecutorService executorService,
                    MessagingOperations operations,
                    InterceptorSupport interceptors,
                     KafkaMessagingClientConfig config) {
            super(operations);
            this.executorService = executorService;
            this.interceptors = interceptors;
            this.config = config;
        }

        @Override
        public <D extends MessagingOperation<D, R>, R> MessagingOperation<D, R> filterForEndpoint(String filter) {
            return new KafkaMessagingOperation(
                    executorService, interceptors, config, filter);
        }
    }
}
