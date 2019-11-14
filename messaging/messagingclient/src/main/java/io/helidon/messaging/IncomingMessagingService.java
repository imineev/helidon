package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.Session;
import java.sql.Connection;

public interface IncomingMessagingService {
    void onIncoming(Message message, Connection connection, Session session); //todo AQJMS specific


    @FunctionalInterface
    interface Incoming
    {
        Message incomingOutgoing(Message message, Connection connection);
    }

    @FunctionalInterface
    interface Outgoing
    {
        void outgoing(Message message, Connection connection);
    }

    @FunctionalInterface
    interface IncomingOutgoing
    {
        Message incomingOutgoing(Message message, Connection connection);
    }

}
