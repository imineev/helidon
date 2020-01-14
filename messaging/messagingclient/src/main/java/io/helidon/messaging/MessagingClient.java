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
import java.util.function.Function;
import java.util.logging.Logger;

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
    Channels channels = Channels.getInstance();
    public static boolean isIncoming = false;

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
                String channelname = striped.substring(0, striped.indexOf("."));
                String channelProperty = striped.substring(channelname.length() + 1);
                if (channelProperty.equals("connector")) {
                    String connectorName = config.getValue(propertyName, String.class);
                    IncomingConnectorFactory incomingConnectorFactory = incomingConnectorFactories.get(connectorName);
                    channels.addIncomingConnectorFactory(channelname, connectorName,
                            incomingConnectorFactory);
                }
            } else if (propertyName.startsWith(ConnectorFactory.OUTGOING_PREFIX)) {
                String striped = propertyName.substring(
                        ConnectorFactory.OUTGOING_PREFIX.length());
                String channelname = striped.substring(0, striped.indexOf("."));
                String channelProperty = striped.substring(channelname.length() + 1);
                if (channelProperty.equals("connector")) {
                    String connectorName = config.getValue(propertyName, String.class);
                    OutgoingConnectorFactory outgoingConnectorFactory = outgoingConnectorFactories.get(connectorName);
                    channels.addOutgoingConnectorFactory(channelname, connectorName,
                            outgoingConnectorFactory);
                }
            }
        }
    }

    public void incoming(IncomingMessagingService incomingMessagingService, String channelname) {
        incoming(incomingMessagingService, channelname, null, false);
    }

    /**
     * Sets up listener for incoming messages on specified channel.
     * @param incomingMessagingService IncomingMessagingService implementation provided is executed when message is received.
     * @param channelname Name of channel to listen on.
     * @param acknowledgement org.eclipse.microprofile.reactive.messaging.Acknowledgement
     */
    public void incoming(IncomingMessagingService incomingMessagingService, String channelname,
                         Acknowledgment.Strategy acknowledgement, boolean isAQ) {
        isIncoming = true;
        IncomingSubscriber incomingSubscriber = new IncomingSubscriber(incomingMessagingService, channelname);
        IncomingConnectorFactory incomingConnectorFactory = channels.getIncomingConnectorFactory(channelname);
        incomingSubscriber.addIncomingConnectionFactory(incomingConnectorFactory);
        if (isAQ) incomingSubscriber.setAQ(true); //todo temp hack
        incomingSubscriber.subscribe(new ChannelSpecificConfig(config, channelname, ConnectorFactory.INCOMING_PREFIX), null);
    }


    /**
     * Sends message on specified channel.
     * @param outgoingMessagingService OutgoingMessagingService implementation provided is executed before message is sent.
     * @param channelname Name of channel to send on.
     */
    public void outgoing(OutgoingMessagingService outgoingMessagingService, String channelname) {
        outgoing(outgoingMessagingService, channelname, false);
    }

//    public void outgoing(String channel, Message message) {
//        call outgoing(String channel, Function<Session, Message> sender);
//    }

    public void outgoing(String channelname, Function<Session, Message> outgoingMessagingService) {
        boolean isAQ = false; //todo this would of course be in config/builder
        channels.addOutgoingMessagingFunction(channelname, outgoingMessagingService);
        new Outgoing(channelname, config, isAQ).outgoing();
    }

    public void outgoing(OutgoingMessagingService outgoingMessagingService, String channelname,
                         boolean isAQ) {
        channels.addOutgoingMessagingService(channelname, outgoingMessagingService);
        new Outgoing(channelname, config, isAQ).outgoing();
    }

    /**
     * Sets up listener for incoming messages on specified channel and in reaction sends message on specified channel.
     * @param processingMessagingService ProcessingMessagingService implementation provided is executed when
     *                                   when message is received and before message is sent.
     * @param incomingchannelname Name of channel to listen on.
     * @param outgoingchannelname Name of channel to send on.
     * @param acknowledgement org.eclipse.microprofile.reactive.messaging.Acknowledgement
     */
    public void incomingoutgoing(ProcessingMessagingService processingMessagingService, String incomingchannelname,
                                 String outgoingchannelname, Acknowledgment.Strategy acknowledgement, boolean isAQ) {
        IncomingSubscriber incomingSubscriber = new IncomingSubscriber(processingMessagingService, incomingchannelname);
        IncomingConnectorFactory incomingConnectorFactory = channels.getIncomingConnectorFactory(incomingchannelname);
        incomingSubscriber.addIncomingConnectionFactory(incomingConnectorFactory);
        if (isAQ) incomingSubscriber.setAQ(true); //todo temp hack
        incomingSubscriber.subscribe(
                new ChannelSpecificConfig(config, incomingchannelname, ConnectorFactory.INCOMING_PREFIX),
                new Outgoing(outgoingchannelname, config, isAQ));
    }

}
