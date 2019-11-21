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

package io.helidon.examples.messaging.kafka;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.OutgoingMessagingService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import javax.jms.Session;


public class MessagingService {

    String incomingChannelName = "my-incomingchannel";
    String outgoingChannelName = "my-outgoingchannel";

    public static void main(String args[]) throws Exception {
        new MessagingService().test();
    }

    private void test() throws Exception {
        org.eclipse.microprofile.config.Config mpConfig = createConfig();

        MessagingClient messagingClient = MessagingClient.build(mpConfig);
        String testtype = System.getProperty("testtype");
        if (testtype.equals("incoming")) doIncoming(messagingClient);
        if (testtype.equals("outgoing")) doOutgoing(messagingClient);

    }

    private void doOutgoing(MessagingClient messagingClient) throws Exception{
        OutgoingMessagingService outgoingMessagingService = new OutgoingMessagingService(){
            @Override
            public Message onOutgoing(Connection connection, Session session) {
                System.out.println("MessagingService.onOutgoing test" +
                        "connection = [" + connection + "], session = [" + session + "]");
                return null;
            }
        };
        messagingClient.outgoing(outgoingMessagingService, outgoingChannelName, new Message(){
            @Override
            public Object getPayload() {
                return "test";
            }

            @Override
            public CompletionStage<Void> ack() {
                return null;
            }

            @Override
            public Object unwrap(Class unwrapType) {
                return this;
            }
        });
        System.out.println("MessagingService.doOutgoing sleep 1 second so message has time to send...");
        Thread.sleep(1 * 1000);
    }

    private void doIncoming(MessagingClient messagingClient) throws Exception {
        IncomingMessagingService incomingMessagingService =
                (message, connection, session) ->
                        System.out.println("Kafka IncomingMessagingService.onProcessing " +
                                "message:" + message + " connection:" + connection + " session:" + session);
        messagingClient.incoming(incomingMessagingService, incomingChannelName);
        System.out.println("MessagingService.doIncoming sleep 2 minutes to receive messages...");
        Thread.sleep(120 * 1000);
    }

    private Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap();
            private void createValues() {

                String connectorName = "acme.kafka";
                // @Connector("acme.kafka")
                // Helidon SE (not MP standard) equivalent config attribute "type"
                // mp.messaging.connector.acme.kafka.classname=io.helidon.messaging.kafka.connector.KafkaConnector
                values.put(ConnectorFactory.CONNECTOR_PREFIX + connectorName + "." + "classname",
                        "io.helidon.messaging.kafka.connector.KafkaConnector");


                // mp.messaging.incoming.my-channel.connector=acme.kafka
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + ConnectorFactory.CONNECTOR_ATTRIBUTE,
                        connectorName);
                // mp.messaging.incoming.my-channel.bootstrap.servers=localhost:9096
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + "bootstrap.servers",
                        "localhost:9092");
                // mp.messaging.incoming.my-channel.topic=my-topic
                values.put(ConnectorFactory.INCOMING_PREFIX + incomingChannelName + "." + "topic",
                        "demotopic");


                // mp.messaging.outgoing.my-channel.connector=acme.kafka
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + ConnectorFactory.CONNECTOR_ATTRIBUTE,
                        connectorName);
                // mp.messaging.outgoing.my-channel.bootstrap.servers=localhost:9096
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + "bootstrap.servers",
                        "localhost:9092");
                // mp.messaging.outgoing.my-channel.topic=my-topic
                values.put(ConnectorFactory.OUTGOING_PREFIX + outgoingChannelName + "." + "topic",
                        "demotopic");
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
