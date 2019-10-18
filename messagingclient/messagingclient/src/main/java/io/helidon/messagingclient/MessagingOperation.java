package io.helidon.messagingclient;

import java.util.concurrent.CompletionStage;

public interface MessagingOperation<D, R> {


    /**
     * Configure parameters using {@link Object} instance with registered mapper.
     * The operation must use named parameters and configure them from the map provided by mapper.
     *
     * @param parameters {@link Object} instance containing parameters
     * @param <T>        type of the parameters
     * @return updated messaging operation
     */
    <T> D namedParam(T parameters);

    CompletionStage<Message> execute();
}
