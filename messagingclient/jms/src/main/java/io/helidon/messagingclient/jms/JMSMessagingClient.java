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
    private final MessagingChannels channels;
    private final ConnectionPool connectionPool;

    JMSMessagingClient(JMSMessagingClientProviderBuilder builder) {
        this.config = builder.messagingConfig();
        this.executorService = builder.executorService();
        this.connectionPool = builder.connectionpool();
        System.out.println("JMSMessagingClient config.topic():" + config.topic());
        System.out.println("JMSMessagingClient config.queue():" + config.queue());
        System.out.println("JMSMessagingClient config.url():" + config.url());
        System.out.println("JMSMessagingClient connectionpool:" + connectionPool);
        System.out.println("JMSMessagingClient config.numberofmessagestoconsume():" + config.numberofmessagestoconsume());
        this.interceptors = builder.interceptors();
        this.channels = builder.channels();
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
    public <T> T channel(Function<MessagingChannelOptions, T> executor) {
        return executor.apply(
                new JMSExecute(executorService, channels, interceptors, config, connectionPool));
    }

    private class JMSExecute extends AbstractMessagingExecute implements MessagingChannelOptions {
        private final ExecutorService executorService;
        private final InterceptorSupport interceptors;
        private final JMSMessagingClientConfig config;
        private final ConnectionPool connectionPool;

        JMSExecute(ExecutorService executorService,
                   MessagingChannels channels,
                   InterceptorSupport interceptors,
                   JMSMessagingClientConfig config, ConnectionPool connectionPool) {
            super(channels);
            this.executorService = executorService;
            this.interceptors = interceptors;
            this.config = config;
            this.connectionPool = connectionPool;
        }

        @Override
        public <D extends MessagingChannel<D, R>, R> MessagingChannel<D, R> filterForEndpoint(String filter) {
            return new JMSMessagingChannel(executorService, interceptors, connectionPool, config);
        }
    }
}
