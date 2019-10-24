package io.helidon.messagingclient;

public interface MessageProcessor {
    Object processMessage(HelidonMessage message);

//    Object processMessage(Session session, HelidonMessage message);
    Object processMessage(Object session, HelidonMessage message);
}
