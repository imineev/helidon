package io.helidon.messaging.jms;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;

public class JMSMessage implements Message {

    @Override
    public Object getPayload() {
        return null;
    }

    @Override
    public CompletionStage<Void> ack() {
        return null;
    }

    @Override
    public Object unwrap(Class unwrapType) {
        return null;
    }
}
