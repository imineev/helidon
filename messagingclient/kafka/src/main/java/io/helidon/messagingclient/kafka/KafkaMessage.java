package io.helidon.messagingclient.kafka;

import io.helidon.messagingclient.HelidonMessage;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;

public class KafkaMessage implements HelidonMessage, Message {
    private String messageString;

    public KafkaMessage(String messageString) {
        this.messageString = messageString;
    }

    public String getString() {
        return messageString;
    }

    @Override
    public Object getPayload() {
        return messageString;
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
