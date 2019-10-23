package io.helidon.messagingclient;

import java.util.concurrent.CompletionStage;

public interface MessagingChannel<D, R> {

    CompletionStage<HelidonMessage> incoming(MessageProcessor testMessageProcessor);

    CompletionStage<HelidonMessage> outgoing(MessageProcessor testMessageProcessor, HelidonMessage message);

}
