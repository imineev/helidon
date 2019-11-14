/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.messaging;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;


public class MessagingClient {


    private static final Logger LOGGER = Logger.getLogger(MessagingClient.class.getName());
    static MessagingClient messagingClient;
    static org.eclipse.microprofile.config.Config config;
    Map<String, IncomingConnectorFactory> incomingConnectorFactories = new HashMap<>();
    Map<String, OutgoingConnectorFactory> outgoingConnectorFactories = new HashMap<>();
    Channels channels = new Channels();

    private MessagingClient() {
    }

    public static MessagingClient build(org.eclipse.microprofile.config.Config config) {
        LOGGER.fine(() -> String.format("MessagingClient..."));
        messagingClient = new MessagingClient();
        messagingClient.config = config;
        messagingClient.initConnectors();
        messagingClient.initChannels();
        return messagingClient;
    }

    private void initConnectors() {
        System.out.println("MessagingClient.initConnectors...");
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(ConnectorFactory.CONNECTOR_PREFIX)) {
                try {
                    String connectorname = propertyName.substring(
                            ConnectorFactory.CONNECTOR_PREFIX.length(), propertyName.lastIndexOf("."));
                    String classname = config.getValue(propertyName, String.class);
                    Class<?> connectorClass = Class.forName(classname);
                    Constructor<?> ctor = connectorClass.getConstructor();
                    Object connectorFactory = ctor.newInstance();
                    if (connectorFactory instanceof IncomingConnectorFactory) {
                        incomingConnectorFactories.put(connectorname, (IncomingConnectorFactory) connectorFactory);
                    }
                    if (connectorFactory instanceof OutgoingConnectorFactory) {
                        outgoingConnectorFactories.put(connectorname, (OutgoingConnectorFactory) connectorFactory);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void initChannels() {
        System.out.println("MessagingClient.initChannels...");
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(ConnectorFactory.INCOMING_PREFIX)) {
                String striped = propertyName.substring(
                        ConnectorFactory.INCOMING_PREFIX.length());
                String channelname =  striped.substring(0, striped.indexOf("."));
                String channelProperty = striped.substring(channelname.length() + 1);
                if (channelProperty.equals("connector")) {
                    String connectorName = config.getValue(propertyName, String.class);
                    IncomingConnectorFactory incomingConnectorFactory = incomingConnectorFactories.get(connectorName);
                    channels.addIncomingConnectorFactory(channelname, connectorName,
                            incomingConnectorFactory);
                }
            }
            else if (propertyName.startsWith(ConnectorFactory.OUTGOING_PREFIX)) {
                System.out.println("MessagingClient.initChannels OUTGOING_PREFIX");
                String striped = propertyName.substring(
                        ConnectorFactory.OUTGOING_PREFIX.length());
                String channelname =  striped.substring(0, striped.indexOf("."));
                String channelProperty = striped.substring(channelname.length() + 1);
                if (channelProperty.equals("connector")) {
                    String connectorName = config.getValue(propertyName, String.class);
                    OutgoingConnectorFactory outgoingConnectorFactory = outgoingConnectorFactories.get(connectorName);
                    System.out.println("MessagingClient.initChannels OUTGOING_PREFIX outgoingConnectorFactory:" + outgoingConnectorFactory);
                    channels.addOutgoingConnectorFactory(channelname, connectorName,
                            outgoingConnectorFactory);
                }
            }
        }
    }

    public void incoming(IncomingMessagingService incomingMessagingService, String channelname) {
        incoming(incomingMessagingService, channelname, null, false);
    }

    public void incoming(IncomingMessagingService incomingMessagingService, String channelname,
                         Acknowledgment.Strategy acknowledgement, boolean isAQ) {
        IncomingSubscriber incomingSubscriber = new IncomingSubscriber(incomingMessagingService, channelname);
        IncomingConnectorFactory incomingConnectorFactory = channels.getIncomingConnectorFactory(channelname);
        incomingSubscriber.addIncomingConnectionFactory(incomingConnectorFactory);
        if (isAQ) incomingSubscriber.setAQ(true); //todo temp hack
        incomingSubscriber.subscribe(new ChannelSpecificConfig(config, channelname, ConnectorFactory.INCOMING_PREFIX));
    }


    public void outgoing(OutgoingMessagingService outgoingMessagingService, String channelname, Message message) {
        outgoing(outgoingMessagingService, channelname, message, false);
    }
    public void outgoing(OutgoingMessagingService outgoingMessagingService, String channelname, Message message,
                         boolean isAQ) {
        OutgoingPublisher outgoingPublisher = new OutgoingPublisher(outgoingMessagingService, channelname);
        OutgoingConnectorFactory outgoingConnectorFactory = channels.getOutgoingConnectorFactory(channelname);
        outgoingPublisher.addOutgoingConnectionFactory(outgoingConnectorFactory);
        if (isAQ) outgoingPublisher.setAQ(true); //todo temp hack
        outgoingPublisher.publish(new ChannelSpecificConfig(config, channelname, ConnectorFactory.OUTGOING_PREFIX), message);
    }

    private class ChannelSpecificConfig implements Config {
        HashMap<String, String> values = new HashMap();

        public ChannelSpecificConfig(Config config, String channelname, String connfactoryPrefix) {
            for (String propertyName : config.getPropertyNames()) {
                if(propertyName.startsWith(connfactoryPrefix) ) {
                    String striped = propertyName.substring(connfactoryPrefix.length());
                    if(striped.substring(0, striped.indexOf(".")).equals(channelname))
                        values.put(propertyName, config.getValue(propertyName, String.class));
                }
            }
        }

        @Override
        public <T> T getValue(String propertyName, Class<T> propertyType) {
            return (T) values.get(propertyName);
        }

        @Override
        public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
            return Optional.empty();
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public Iterable<ConfigSource> getConfigSources() {
            return null;
        }
    }

}
