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

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;
import io.helidon.messagingclient.*;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;


public class MessagingService implements Service {

    private static final Logger LOGGER = Logger.getLogger(MessagingService.class.getName());

    private final MessagingClient messagingClient;
    private boolean isCompensate;

    MessagingService(MessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    MessagingService(MessagingClient messagingClient, MessagingClient messagingClient2) {
        this.messagingClient = messagingClient;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::noop)
//                .post("/", Handler.create(MyFilter.class, this::listenForMessagesPOST))
                .get("/outgoing", this::outgoing)
                .get("/incoming", this::incoming)
                .get("/incomingoutgoing", this::incomingoutgoing)
                .get("/reserve", this::incomingoutgoingreserve)
                .get("/compensate", this::incomingoutgoingcompensate);
    }


    private void noop(ServerRequest request, ServerResponse response) {
        System.out.println("options: outgoing, incoming, incomingoutgoing, ..");
    }

    private void incoming(ServerRequest request, ServerResponse response) {
        System.out.println("incoming");
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/param subscribe(java.util.regex.Pattern
                .incoming(new TestMessageProcessorIncoming()))
                .thenAccept(messageReceived -> postRecieveProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    class TestMessageProcessorIncoming implements MessageProcessor {
        @Override
        public Object processMessage(HelidonMessage message) {
            return processMessage(null, message);
        }

        @Override
        public Object processMessage(Object session, HelidonMessage message) {
            System.out.println("TestMessageProcessorIncoming.processMessage message:" + message);
        
            return message + "sent";
        }
    }

    private void postRecieveProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("MessagingService.postRecieveProcessMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }


    private void outgoing(ServerRequest request, ServerResponse response) {
        System.out.println("outgoing/send message via producer");
        String message = "kafkatest messaging";
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/param subscribe(java.util.regex.Pattern
                .outgoing(new TestMessageProcessorOutgoing(), () -> message))
                .thenAccept(messageReceived -> postSendProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
        response.send(" message sent:" + message);
    }

    class TestMessageProcessorOutgoing implements MessageProcessor {
        @Override
        public Object processMessage(HelidonMessage message) {
            return processMessage(null, message);
        }

        @Override
        public Object processMessage(Object session, HelidonMessage message) {
            System.out.println("TestMessageProcessorOutgoing.processMessage message:" + message);
            //todo insert db row, etc.
            return message + "sent";
        }
    }

    private void postSendProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("MessagingService.postSendProcessMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }


    //todo this would be based on the message action not the rest call url...
    private void incomingoutgoingreserve(ServerRequest request, ServerResponse response) {
        isCompensate = false;
        incomingoutgoing(request, response);
    }

    //todo this would be based on the message action not the rest call url...
    private void incomingoutgoingcompensate(ServerRequest request, ServerResponse response) {
        isCompensate = true;
        incomingoutgoing(request, response);
    }

    private void incomingoutgoing(ServerRequest request, ServerResponse response) {
        System.out.println("incomingoutgoing...");
        String outgoingmessaging = "kafkatest outgoing messaging resulting/translating from incoming message";
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/param subscribe(java.util.regex.Pattern
                .incoming(new TestMessageProcessorIncomingOutgoing(response, outgoingmessaging), false))
                .thenAccept(messageReceived -> postRecieveProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
        response.send("message received and message sent:" + outgoingmessaging);
    }


    class TestMessageProcessorIncomingOutgoing implements MessageProcessor {
        ServerResponse response;
        String outgoingmessage;

        public TestMessageProcessorIncomingOutgoing(ServerResponse response, String outgoingmessage) {
            this.response = response;
            this.outgoingmessage = outgoingmessage;
        }

        @Override
        public Object processMessage(HelidonMessage message) {
            return processMessage(null, message);
        }

        @Override
        public Object processMessage(Object session, HelidonMessage message) {
            System.out.println("TestMessageProcessorIncomingOutgoing.processMessage message:" + message);
            messagingClient.channel(exec -> exec
                    .filterForEndpoint("kafkasubscribewithpattern")//todo get from config or param subscribe(java.util.regex.Pattern
                    .outgoing(new TestMessageProcessorOutgoing(), () -> outgoingmessage, session, "inventoryqueue")) //todo get from config or param
                    .thenAccept(outgoingMessage -> postSendProcessMessage(response, outgoingMessage))
                    .exceptionally(throwable -> sendError(throwable, response));
            // todo this should be the one sent not the one above...   response.send("sent message: " + outgoingmessage);

            return message + "sent";
        }
    }


    private Void sendError(final Throwable throwable, ServerResponse response) {
        Throwable toLog = throwable;
        if (throwable instanceof CompletionException) {
            toLog = throwable.getCause();
        }
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        response.send("Failed to process request: " + toLog.getClass().getName() + "(" + toLog.getMessage() + ")");
        LOGGER.log(Level.WARNING, "Failed to process request", throwable);
        return null;
    }




    private void incomingoutgoingchannels(ServerRequest request, ServerResponse response) {
        // incoming("channelname", TestMessageProcessorIncomingOutgoing)

        System.out.println("incomingoutgoing...");
        String outgoingmessaging = "kafkatest outgoing messaging resulting/translating from incoming message";
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/param subscribe(java.util.regex.Pattern
                .incoming(new TestMessageProcessorIncomingOutgoing(response, outgoingmessaging), false))
                .thenAccept(messageReceived -> postRecieveProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
        response.send("message received and message sent:" + outgoingmessaging);
    }






//    @Incoming("in")
//    @Outgoing("out")
//    public PublisherBuilder<Message<I>, Message<O>> process() {
////        return null;
//        return ReactiveStreams.<Message<I>>builder().map(this::convert);
//    }
//
//    private <S> S convert(Message<I> iMessage) {
//        return null;
//    }


//    @Incoming("sink")
    public void sink(int val) {
//        list.add(val);
    }


    private AtomicInteger counter = new AtomicInteger();

//    @Incoming("data")
//    @Outgoing("output-2")
//    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
//    public KafkaMessage<String, Integer> process(Message<Integer> input) {
//        KafkaMessage<String, Integer> message = KafkaMessage.of(
//                Integer.toString(input.getPayload()), input.getPayload() + 1).withAck(input::ack);
//        message.getHeaders().put("hello", "clement").put("count", Integer.toString(counter.incrementAndGet()));
//        return message;
//    }
//
//    public void testincomingcoutgoing() {
//        KafkaMessage<String, Integer> process = process(new Message<Integer>() {
//            @Override
//            public Integer getPayload() {
//                return 1;
//            }
//        });
//    }


    public static void main(String args[]) throws Exception {
        new MessagingService(null).init();
    }

    private void init() throws Exception {
        org.eclipse.microprofile.config.Config mpConfig = new org.eclipse.microprofile.config.Config(){

            HashMap<String, String> values = new HashMap();
            private void createValues() {
                values.put(ConnectorFactory.INCOMING_PREFIX + ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, "my-channel");
                values.put(ConnectorFactory.CONNECTOR_PREFIX + ConnectorFactory.CONNECTOR_ATTRIBUTE, "acme.kafka");
            }
            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                return null;
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                return Optional.empty();
            }

            @Override
            public Iterable<String> getPropertyNames() {
                return null;
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };

    /*
    @Connector("acme.kafka")
    mp.messaging.incoming.my-channel.connector=acme.kafka
    mp.messaging.incoming.my-channel.bootstrap.servers=localhost:9096
    mp.messaging.incoming.my-channel.topic=my-topic
    mp.messaging.connector.acme.kafka.bootstrap.servers=localhost:9092
     */
        MessagingClient.builder(mpConfig)
                // will find IncomingConnectorFactory and OutgoingConnectorFactory instances,
                //  call getPublisherBuilder and getSubscriberBuilder on them
                // these will return org.reactivestreams.Publisher that is backed by
                //   submissionPublisher.subscribe(new Flow.Subscriber()
                // and org.reactivestreams.Subscriber
//        MessagingClient.builder().build()
                .channel("my-channel")
//                .acknowledgments(Acknowledgment.Strategy.NONE)
                .incoming(new IncomingMessagingMethods()); // takes one of the 3 interfaces below

    }

    class IncomingMessagingMethods implements MessageProcessor{ //todo add marker interface for Incoming, Outgoing, and incomingoutgoing
        void someincomingmethod() {

        }

        // incoming void method(I payload)

        // outgoing Publisher<Message<O>> method()

        @Override //no longer applicable ...
        public Object processMessage(HelidonMessage message) {
            return null;
        }

        @Override //no longer applicable ...
        public Object processMessage(Object session, HelidonMessage message) {
            return null;
        }
    }

}
