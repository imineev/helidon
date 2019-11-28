package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;

import java.util.HashMap;
import java.util.Map;

public class Channels {

    //todo cleanup/consolidate...
    Map<String, IncomingConnectorFactoryHolder> incomingConnectorFactoryHolderMap = new HashMap<>();
    Map<String, OutgoingConnectorFactoryHolder> outgoingConnectorFactoryHolderMap = new HashMap<>();
    Map<String, OutgoingMessagingService> outgoingMessagingServiceMap = new HashMap<>();
    Map<String, ProcessingMessagingService> processingMessagingServiceMap = new HashMap<>();
    Map<String, ProcessingMessagingServiceHolder> processingMessagingServiceHolderMap = new HashMap<>();
    static Channels instance;

    private Channels(){
    }

    public static Channels getInstance() {
        return instance == null? instance = new Channels():instance;
    }


    public void addIncomingConnectorFactory(String channelName, String connectionFactoryName,
                                            IncomingConnectorFactory incomingConnectorFactory) {
        this.incomingConnectorFactoryHolderMap.put(
                channelName, new IncomingConnectorFactoryHolder(connectionFactoryName, incomingConnectorFactory));
    }

    public IncomingConnectorFactory getIncomingConnectorFactory(String channelName) {
        return incomingConnectorFactoryHolderMap.get(channelName).getIncomingConnectorFactory();
    }

    public void addOutgoingConnectorFactory(String channelName, String connectionFactoryName, OutgoingConnectorFactory outgoingConnectorFactory) {
        this.outgoingConnectorFactoryHolderMap.put(channelName, new OutgoingConnectorFactoryHolder(connectionFactoryName, outgoingConnectorFactory));
    }

    public OutgoingConnectorFactory getOutgoingConnectorFactory(String channelName) {
        return outgoingConnectorFactoryHolderMap.get(channelName).getOutgoingConnectorFactory();
    }






    public void addOutgoingMessagingService(String channelname, OutgoingMessagingService outgoingMessagingService) {
        this.outgoingMessagingServiceMap.put(channelname, outgoingMessagingService);
    }

    public OutgoingMessagingService getOutgoingMessagingService(String channelname) {
        return outgoingMessagingServiceMap.get(channelname);
    }




    public void addProcessingMessagingService(String channelname, ProcessingMessagingService processingMessagingService) {
        this.processingMessagingServiceMap.put(channelname, processingMessagingService);
    }

    public ProcessingMessagingService getProcessingMessagingService(String channelname) {
        return processingMessagingServiceMap.get(channelname);
    }

    public void addProcessingMessagingServiceHolder(String channelName, Message message, Session session, Connection connection) {
        processingMessagingServiceHolderMap.put(channelName, new ProcessingMessagingServiceHolder(message, session,connection));
    }

    public ProcessingMessagingServiceHolder getProcessingMessagingServiceHolder(String channelname) {
        return processingMessagingServiceHolderMap.get(channelname);
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

    public class ProcessingMessagingServiceHolder {

        private Message message;
        private Session session;
        private Connection connection;

        public Message getMessage() {
            return message;
        }

        public Session getSession() {
            return session;
        }

        public Connection getConnection() {
            return connection;
        }

        ProcessingMessagingServiceHolder(Message message, Session session, Connection connection) {
            this.message = message;
            this.session = session;
            this.connection = connection;
        }

    }
}
