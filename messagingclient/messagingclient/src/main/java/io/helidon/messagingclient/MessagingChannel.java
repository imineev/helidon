package io.helidon.messagingclient;

import java.util.concurrent.CompletionStage;

public interface MessagingChannel<D, R> {

    CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor);

    // todo AQ/session specific move to sub
    CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor, boolean isCloseSession);

    //todo MessageProcessor could be of use for outgoing but not really necessary
    CompletionStage<HelidonMessage> outgoing(MessageProcessor messageProcessor, HelidonMessage message);

    //todo session may only make sense for JMS and AQ at that so may want to have eg AQMessagingChannel sub
    CompletionStage<HelidonMessage> outgoing(
            MessageProcessor messageProcessor, HelidonMessage message, Object session, String queueName);

}
