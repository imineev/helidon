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
    private final MessagingChannels channels;

    KafkaMessagingClient(KafkaMessagingClientProviderBuilder builder) {
        this.config = builder.messagingConfig();
        this.executorService = builder.executorService();
        System.out.println("KafkaMessagingClient config.topic():" + config.topic());
        System.out.println("KafkaMessagingClient config.bootstrapservers():" + config.bootstrapservers());
        System.out.println("KafkaMessagingClient config.numberofmessagestoconsume():" + config.numberofmessagestoconsume());
        this.interceptors = builder.interceptors();
        this.channels = builder.channels();
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
    public <T> T channel(Function<MessagingChannelOptions, T> executor) {
        return executor.apply(
                new KafkaExecute(executorService, channels, interceptors, config));
    }

    private class KafkaExecute extends AbstractMessagingExecute implements MessagingChannelOptions {
        private final ExecutorService executorService;
        private final InterceptorSupport interceptors;
        private final KafkaMessagingClientConfig config;

        KafkaExecute(ExecutorService executorService,
                    MessagingChannels channels,
                    InterceptorSupport interceptors,
                     KafkaMessagingClientConfig config) {
            super(channels);
            this.executorService = executorService;
            this.interceptors = interceptors;
            this.config = config;
        }

        @Override
        public <D extends MessagingChannel<D, R>, R> MessagingChannel<D, R> filterForEndpoint(String filter) {
            return new KafkaMessagingChannel(
                    executorService, interceptors, config, filter);
        }
    }
}
