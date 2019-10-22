package io.helidon.messagingclient;

public interface MessagingChannelOptions {
    <D extends MessagingChannel<D, R>, R> MessagingChannel<D,R> filterForEndpoint(String filter);
}
