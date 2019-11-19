package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.Session;
import java.sql.Connection;

public interface ProcessingMessagingService {
    Message onIncoming(Message message, Connection connection, Session session) throws Exception; //todo AQ/JMS specific
}
