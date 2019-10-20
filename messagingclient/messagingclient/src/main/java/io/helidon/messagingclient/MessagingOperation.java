package io.helidon.messagingclient;

import java.util.concurrent.CompletionStage;

public interface MessagingOperation<D, R> {

    CompletionStage<Message> incoming(MessageProcessor testMessageProcessor);

    CompletionStage<Message> outgoing(MessageProcessor testMessageProcessor);

}
