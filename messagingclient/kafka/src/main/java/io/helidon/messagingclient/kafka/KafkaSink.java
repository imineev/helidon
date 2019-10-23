package io.helidon.messagingclient.kafka;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

public class KafkaSink {
    public KafkaSink(String servers) {

    }

    public SubscriberBuilder<? extends Message<?>, Void> getSink() {
        return null;
    }
}
