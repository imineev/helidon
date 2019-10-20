package io.helidon.messagingclient.kafka;

import io.helidon.messagingclient.Message;

public class KafkaMessage implements Message {
    private String messageString;

    public KafkaMessage(String messageStringn) {
        this.messageString = messageString;
    }

    @Override
    public String getString() {
        return messageString;
    }

}
