package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.Session;
import java.sql.Connection;

public interface OutgoingMessagingService {
    void onOngoing(Message message, Connection connection, Session session); //todo AQJMS specific
}
