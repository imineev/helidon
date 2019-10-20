package io.helidon.messagingclient;

public interface MessageProcessor {
    Object processMessage(Message message);

    Object processMessage(Session session, Message message);
}
