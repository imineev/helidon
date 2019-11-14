package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.sql.Connection;
import java.util.concurrent.CompletionStage;

//todo this is AQ JMS specific
public class MessageWithConnectionAndSession<K, V> implements Message<javax.jms.Message> {
    javax.jms.Message payload;
    Connection connection;
    javax.jms.Session session;

    public MessageWithConnectionAndSession(
            javax.jms.Message payload, Connection connection, javax.jms.Session session,
                          K key,
            V value) {
        this.payload = payload;
        this.connection = connection;
        this.session = session;
    }

    Connection getConnection() {
        return connection;
    }

    javax.jms.Session getSession() {
        return session;
    }

    @Override
    public javax.jms.Message getPayload() {
        return payload;
    }

    @Override
    public CompletionStage<Void> ack() {
        return null;
    }

    @Override
    public Object unwrap(Class unwrapType) {
        return this;
    }
}
