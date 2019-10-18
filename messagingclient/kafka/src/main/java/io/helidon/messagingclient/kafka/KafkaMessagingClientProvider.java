package io.helidon.messagingclient.kafka;

import io.helidon.messagingclient.spi.MessagingClientProvider;

public class KafkaMessagingClientProvider  implements MessagingClientProvider {
    static final String MESSAGING_TYPE = "kafka";

    static {
        System.out.println("KafkaMessagingClientProvider.static initializer MESSAGING_TYPE:" + MESSAGING_TYPE);
    }

    @Override
    public String name() {
        return MESSAGING_TYPE;
    }

    @Override
    public KafkaMessagingClientProviderBuilder builder() {
        return new KafkaMessagingClientProviderBuilder();
    }

}
