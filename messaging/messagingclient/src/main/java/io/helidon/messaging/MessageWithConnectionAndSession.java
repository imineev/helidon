package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;

/**
 * Created by JMSConsumer
 */
public class MessageWithConnectionAndSession<K, V> implements Message<Object> {
    String channelname;
    Message payload;
    Connection connection;
    Session session;

    public MessageWithConnectionAndSession(String channelname,
            Message payload, Connection connection, Session session, //todo it's not really the payload it's the message itself
                          K key, V value) {
        this.channelname = channelname;
        this.payload = payload;
        this.connection = connection;
        this.session = session;
    }

    public Connection getConnection() {
        return connection;
    }

    public Session getSession() {
        return session;
    }

    public String getChannelName() {
        return channelname; // todo current assumption/limitation is that outgoing is same channel as incoming
    }

    public Object getMessage(Class unwrapType) { //todo unlike session and connection, unwraps before returning, should be consistent
        return payload.unwrap(unwrapType);
    }

    @Override
    public Message getPayload() {
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
