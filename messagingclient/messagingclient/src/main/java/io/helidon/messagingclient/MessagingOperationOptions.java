package io.helidon.messagingclient;

public interface MessagingOperationOptions {
    <D extends MessagingOperation<D, R>, R> MessagingOperation<D,R> filterForEndpoint(String filter);
}
