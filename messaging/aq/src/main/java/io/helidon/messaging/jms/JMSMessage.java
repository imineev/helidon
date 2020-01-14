package io.helidon.messaging.jms;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class JMSMessage implements Message {

    private javax.jms.Message message;

    public JMSMessage(javax.jms.Message message) {
        this.message = message;
    }

    //todo above contructor is for internal and this is convenience for application, move or remove...
    public JMSMessage() {

    }

    public javax.jms.Message getPayload() {
        return message;
    }

    public CompletionStage<Void> ack() {
        //TODO: implement acknowledge
        return new CompletableFuture<>();
    }

    @Override
    public Object unwrap(Class unwrapType) { //todo
        return message;
    }
}
