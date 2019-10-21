package io.helidon.messagingclient.jms;

import io.helidon.messagingclient.*;
import io.helidon.messagingclient.common.InterceptorSupport;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class JMSMessagingClient implements MessagingClient {
    private final JMSMessagingClientConfig config;
    private final ExecutorService executorService;
    private final InterceptorSupport interceptors;
    private final MessagingOperations operations;
    private final ConnectionPool connectionPool;

    JMSMessagingClient(JMSMessagingClientProviderBuilder builder) {
        this.config = builder.messagingConfig();
        this.executorService = builder.executorService();
        this.connectionPool = builder.connectionpool();
        System.out.println("JMSMessagingClient config.topic():" + config.topic());
        System.out.println("JMSMessagingClient config.queue():" + config.queue());
        System.out.println("JMSMessagingClient config.url():" + config.url());
        System.out.println("JMSMessagingClient config.numberofmessagestoconsume():" + config.numberofmessagestoconsume());
        this.interceptors = builder.interceptors();
        this.operations = builder.operations();
    }

    @Override
    public CompletionStage<Void> ping() {
        return null;
    }

    @Override
    public String messagingType() {
        return "jms";
    }

    @Override
    public <T> T operation(Function<MessagingOperationOptions, T> executor) {
        return executor.apply(
                new JMSExecute(executorService, operations, interceptors, config, connectionPool));
    }

    private class JMSExecute extends AbstractMessagingExecute implements MessagingOperationOptions {
        private final ExecutorService executorService;
        private final InterceptorSupport interceptors;
        private final JMSMessagingClientConfig config;
        private final ConnectionPool connectionPool;

        JMSExecute(ExecutorService executorService,
                   MessagingOperations operations,
                   InterceptorSupport interceptors,
                   JMSMessagingClientConfig config, ConnectionPool connectionPool) {
            super(operations);
            this.executorService = executorService;
            this.interceptors = interceptors;
            this.config = config;
            this.connectionPool = connectionPool;
        }

        @Override
        public <D extends MessagingOperation<D, R>, R> MessagingOperation<D, R> filterForEndpoint(String filter) {
            return new JMSMessagingOperation(executorService, interceptors, connectionPool, config);
        }
    }
}
