package io.helidon.messagingclient.kafka;

import io.helidon.messagingclient.Message;

public class KafkaMessage implements Message {
    private String messageString;
    private Object session;

    public KafkaMessage(String messageString, Object session) {
        this.messageString = messageString;
        this.session = session;
    }

    @Override
    public String getString() {
        return messageString;
    }

    @Override
    public Object getSession() {
        return session;
    }
}
