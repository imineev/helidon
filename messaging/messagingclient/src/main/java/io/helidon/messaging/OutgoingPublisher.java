package io.helidon.messaging;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;

import java.util.ArrayList;
import java.util.List;

public class OutgoingPublisher {
    private final OutgoingMessagingService outgoingMessagingService;
    private List<OutgoingConnectorFactory> outgoingConnectorFactories = new ArrayList<>();
    boolean isAQ;

    public OutgoingPublisher(OutgoingMessagingService outgoingMessagingService, String channelname) {
        this.outgoingMessagingService = outgoingMessagingService;
    }

    public void addOutgoingConnectionFactory(OutgoingConnectorFactory outgoingConnectorFactory) {
        System.out.println("OutgoingPublisher.addOutgoingConnectionFactory " +
                "outgoingConnectorFactory:" + outgoingConnectorFactory);
        outgoingConnectorFactories.add(outgoingConnectorFactory);
    }

    public void publish(Config config, Message message) {
        System.out.println("OutgoingPublisher.public outgoingConnectorFactory:" +
                outgoingConnectorFactories.get(0));
        outgoingConnectorFactories
                .get(0)
                .getSubscriberBuilder(config);
//                .build();
                //
//                .subscribe(this);
    }

    public void setAQ(boolean b) {
        isAQ = b;
    }
}
