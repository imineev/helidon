package io.helidon.messagingclient.jms;

import io.helidon.messagingclient.spi.MessagingClientProvider;

public class JMSMessagingClientProvider implements MessagingClientProvider {
    static final String MESSAGING_TYPE = "jms";

    static {
        System.out.println("JMSMessagingClientProvider.static initializer MESSAGING_TYPE:" + MESSAGING_TYPE);
    }

    @Override
    public String name() {
        return MESSAGING_TYPE;
    }

    @Override
    public JMSMessagingClientProviderBuilder builder() {
        return new JMSMessagingClientProviderBuilder();
    }

}
