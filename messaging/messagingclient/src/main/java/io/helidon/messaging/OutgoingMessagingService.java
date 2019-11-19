package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.Session;
import java.sql.Connection;


/**
 * Returns message that will be sent
 */
public interface OutgoingMessagingService {
    Message onOutgoing(Connection connection, Session session) throws Exception; //todo AQ/JMS specific
}
