package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Implementation receives message and associated session and connection if/as appropriate whenever a message
 * is received.
 */
public interface IncomingMessagingService {
    void onIncoming(Message message, Connection connection, Session session);
}
