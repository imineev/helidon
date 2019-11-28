package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;


/**
 * Returns message that will be sent
 */
public interface OutgoingMessagingService {
    Message onOutgoing(Connection connection, Session session) throws Exception;
}
