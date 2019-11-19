package io.helidon.messaging;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.reactivestreams.Subscription;

import javax.jms.Session;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class OutgoingPublisher implements Subscription {
    private List<OutgoingConnectorFactory> outgoingConnectorFactories = new ArrayList<>();
    boolean isAQ;
    String channelname;

    public OutgoingPublisher(String channelname) {
        this.channelname = channelname;
    }

    public void addOutgoingConnectionFactory(OutgoingConnectorFactory outgoingConnectorFactory) {
        outgoingConnectorFactories.add(outgoingConnectorFactory);
    }

    public void publish(Config config) {
        outgoingConnectorFactories
                .get(0)
                .getSubscriberBuilder(config)
                .build() //todo no ops...
                .onSubscribe(this);
    }

    public void setAQ(boolean b) {
        isAQ = b;
    }

    @Override
    public void request(long l) {
        System.out.println("--------------->OutgoingPublisher.request l:" + l);
    }

    @Override
    public void cancel() {
        System.out.println("--------------->OutgoingPublisher.cancel");
    }
}
