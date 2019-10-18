package io.helidon.messagingclient;

public interface MessagingListenForMessages {
    <D extends MessagingOperation<D, R>, R> MessagingOperation<D,R> createNamedFilter(String filter);
}
