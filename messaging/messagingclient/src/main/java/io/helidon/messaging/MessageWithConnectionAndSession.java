package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.sql.Connection;
import java.util.concurrent.CompletionStage;

//todo this is AQ JMS specific
public class MessageWithConnectionAndSession<K, V> implements Message<javax.jms.Message> {
    String channelname;
    javax.jms.Message payload;
    Connection connection;
    javax.jms.Session session;

    public MessageWithConnectionAndSession(String channelname,
            javax.jms.Message payload, Connection connection, javax.jms.Session session,
                          K key,
            V value) {
        this.channelname = channelname;
        this.payload = payload;
        this.connection = connection;
        this.session = session;
    }

    public Connection getConnection() {
        return connection;
    }

    public javax.jms.Session getSession() {
        return session;
    }

    public String getChannelName() {
        return channelname; // todo current assumption/limitation is that outgoing is same channel as incoming
        // "inventoryqueue-channel";
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
