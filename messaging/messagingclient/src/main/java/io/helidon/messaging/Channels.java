package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;

import java.util.HashMap;
import java.util.Map;

public class Channels {

    Map<String, IncomingConnectorFactoryHolder> incomingConnectorFactoryHolderHashMap = new HashMap<>();
    Map<String, OutgoingConnectorFactoryHolder> outgoingConnectorFactoryHolderHashMap = new HashMap<>();


    public void addIncomingConnectorFactory(String channelName, String connectionFactoryName,
                                            IncomingConnectorFactory incomingConnectorFactory) {
        this.incomingConnectorFactoryHolderHashMap.put(
                channelName, new IncomingConnectorFactoryHolder(connectionFactoryName, incomingConnectorFactory));
    }

    public IncomingConnectorFactory getIncomingConnectorFactory(String channelName) {
        return incomingConnectorFactoryHolderHashMap.get(channelName).getIncomingConnectorFactory();
    }

    public void addOutgoingConnectorFactory(String channelName, String connectionFactoryName, OutgoingConnectorFactory outgoingConnectorFactory) {
        this.outgoingConnectorFactoryHolderHashMap.put(channelName, new OutgoingConnectorFactoryHolder(connectionFactoryName, outgoingConnectorFactory));
    }

    public OutgoingConnectorFactory getOutgoingConnectorFactory(String channelName) {
        return outgoingConnectorFactoryHolderHashMap.get(channelName).getOutgoingConnectorFactory();
    }

    class IncomingConnectorFactoryHolder {
        String name;
        IncomingConnectorFactory incomingConnectorFactory;

        public IncomingConnectorFactoryHolder(String name, IncomingConnectorFactory incomingConnectorFactory) {
            this.name = name;
            this.incomingConnectorFactory = incomingConnectorFactory;
        }

        public IncomingConnectorFactory getIncomingConnectorFactory() {
            return incomingConnectorFactory;
        }
    }

    class OutgoingConnectorFactoryHolder {
        String name;
        OutgoingConnectorFactory outgoingConnectorFactory;

        public OutgoingConnectorFactoryHolder(String name, OutgoingConnectorFactory outgoingConnectorFactory) {
            this.name = name;
            this.outgoingConnectorFactory = outgoingConnectorFactory;
        }

        public OutgoingConnectorFactory getOutgoingConnectorFactory() {
            return outgoingConnectorFactory;
        }
    }
}
