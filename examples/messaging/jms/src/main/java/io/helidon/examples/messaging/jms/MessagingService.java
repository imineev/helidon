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

package io.helidon.examples.messaging.jms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.OutgoingMessagingService;
import io.helidon.messaging.ProcessingMessagingService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;


public class MessagingService {

    String incomingqueue = System.getProperty("incomingqueue");
    String outgoingqueue = System.getProperty("outgoingqueue");
    String incomingChannelName = incomingqueue + "-channel";
String outgoingChannelName =  outgoingqueue + "-channel";
//    String incomingChannelName = "processor" + "-channel"; // must have same channel name  for processor/incomingoutgoing
//    String outgoingChannelName = "processor" + "-channel"; // must have same channel name  for processor/incomingoutgoing

    public static void main(String args[]) throws Exception {
        new MessagingService().test();
    }

    private void test() throws Exception {
        org.eclipse.microprofile.config.Config mpConfig = createConfig();

        MessagingClient messagingClient = MessagingClient.build(mpConfig);
        String testtype = System.getProperty("testtype");
        if (testtype.equals("incoming")) doIncoming(messagingClient);
        if (testtype.equals("outgoing")) doOutgoing(messagingClient);
        if (testtype.equals("incomingoutgoing")) doIncomingOutgoing(messagingClient);
    }

    private void doIncoming(MessagingClient messagingClient) throws Exception {
        IncomingMessagingService incomingMessagingService =
                (message, connection, session) -> System.out.println("MessagingService.onProcessing" +
                        " connection:" + connection + "Session:" + session);
        messagingClient.incoming(incomingMessagingService, incomingChannelName, null, true);
        System.out.println("MessagingService.doIncoming sleep 2 minutes to receive messages...");
        Thread.sleep(120 * 1000);
    }

    private void doOutgoing(MessagingClient messagingClient) throws Exception {
        OutgoingMessagingService outgoingMessagingService = new OutgoingMessagingService() {
            @Override
            public Message onOutgoing(Connection connection, Session session) throws JMSException {
                System.out.println("MessagingService.onOutgoing test connection:" + connection +
                        "Session:" + session);
                String messageTxt = "outgoinginventorytestmessage";
                TextMessage outgoinginventorytestmessage = session.createTextMessage(messageTxt);
                return new Message(){
                    @Override
                    public Object getPayload() {
                        return messageTxt;
                    }

                    @Override
                    public CompletionStage<Void> ack() {
                        return null;
                    }

                    @Override
                    public Object unwrap(Class unwrapType) {
                        return outgoinginventorytestmessage;
                    }
                };
            }
        };
        messagingClient.outgoing(outgoingMessagingService, outgoingChannelName, true);
        System.out.println("MessagingService.doOutgoing sleep 1 second so message has time to send...");
        Thread.sleep(1 * 1000);
    }

    private void doIncomingOutgoing(MessagingClient messagingClient) throws Exception {
        ProcessingMessagingService processingMessagingService = (message, connection, session) -> {
            System.out.println("-------------->MessagingService.doIncomingOutgoing connection:" + connection +
                    "Session:" + session + " do db work...");
            final int inventorycount;
            ResultSet resultSet = connection.createStatement().executeQuery(
                    "select inventorycount from inventory  where inventoryid = 'inventoryitem1'");
            if (resultSet.next()) {
                inventorycount = resultSet.getInt("inventorycount");
                System.out.println("MessagingService.doIncomingOutgoing inventorycount:" + inventorycount);
                //todo reduce inventory as appropriate
            } else inventorycount = 0;
            try {
                String text = inventorycount > 0 ? "inventoryexists" : "inventorydoesnotexist";
                TextMessage textMessage = session.createTextMessage(text);
                textMessage.setStringProperty("action", text);
                textMessage.setIntProperty("orderid", 66);
                return new Message() {
                    @Override
                    public Object getPayload() {
                        return text;
                    }

                    @Override
                    public CompletionStage<Void> ack() {
                        return null;
                    }

                    @Override
                    public Object unwrap(Class unwrapType) {
                        return textMessage;
                    }
                };
            } catch (JMSException e) {
                e.printStackTrace();
                return null; //todo
            }
        };
        messagingClient.incomingoutgoing(
                processingMessagingService, incomingChannelName, outgoingChannelName, null,  true);
        System.out.println("MessagingService.doIncoming sleep 2 minutes to receive messages...");
        Thread.sleep(120 * 1000);
    }

    private Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap();

            private void createValues() {

                String connectorName = "test.aq";
                // @Connector("acme.aq")
                // Helidon SE (not MP standard) equivalent config attribute "type"
                // mp.messaging.connector.acme.kafka.classname=io.helidon.messaging.kafka.connector.JMSConnector
                values.put(ConnectorFactory.CONNECTOR_PREFIX + connectorName + "." + "classname",
                        "io.helidon.messaging.jms.connector.JMSConnector");


                // mp.messaging.incoming.my-channel.connector=acme.aq
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + ConnectorFactory.CONNECTOR_ATTRIBUTE,
                        connectorName);
                // mp.messaging.incoming.my-channel.url=someurl
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + "url",
                        "xxxxx");
                // mp.messaging.incoming.my-channel.user=someuser
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + "user",
                        "xxxxx");
                // mp.messaging.incoming.my-channel.password=somepassword
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + "password",
                        "xxxxx");
                // mp.messaging.incoming.my-channel.queue=somepassword
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + "queue",
                        incomingqueue);


                // mp.messaging.incoming.my-channel.connector=acme.aq
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + ConnectorFactory.CONNECTOR_ATTRIBUTE,
                        connectorName);
                // mp.messaging.outgoing.my-channel.url=someurl
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + "url",
                        "xxxxxx");
                // mp.messaging.outgoing.my-channel.user=someuser
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + "user",
                        "xxxxxx");
                // mp.messaging.outgoing.my-channel.password=somepassword
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + "password",
                        "xxxxxx");
                // mp.messaging.outgoing.my-channel.queue=somepassword
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + "queue",
                        outgoingqueue);
            }

            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                createValues();
                return (T) values.get(propertyName);
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                createValues();
                return Optional.empty();
            }

            @Override
            public Iterable<String> getPropertyNames() {
                createValues();
                return values.keySet();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };
    }


}
