package io.helidon.messaging;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;

public class Outgoing {

    String channelname;
    private Config config;
    boolean isAQ;

    public Outgoing(String channelname, Config config, boolean isAQ) {
        this.channelname = channelname;
        this.config = config;
        this.isAQ = isAQ;
    }

    public void outgoing() {
        OutgoingPublisher outgoingPublisher = new OutgoingPublisher(channelname);
        OutgoingConnectorFactory outgoingConnectorFactory = Channels.getInstance().getOutgoingConnectorFactory(channelname);
        outgoingPublisher.addOutgoingConnectionFactory(outgoingConnectorFactory);
        if (isAQ) outgoingPublisher.setAQ(true); //todo temp hack
        outgoingPublisher.publish(new ChannelSpecificConfig(
                config, channelname, ConnectorFactory.OUTGOING_PREFIX));
    }

}
