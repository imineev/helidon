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

import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;
import io.helidon.messagingclient.*;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;


public class PokemonService implements Service {

    private static final Logger LOGGER = Logger.getLogger(PokemonService.class.getName());

    private final MessagingClient messagingClient;

    PokemonService(MessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::noop)
//                .post("/", Handler.create(Pokemon.class, this::listenForMessagesPOST))
                .get("/outgoing", this::outgoing)
                .get("/incoming", this::incoming)
                .get("/incomingoutgoing", this::incomingoutgoing);
    }


    private void noop(ServerRequest request, ServerResponse response) {
        System.out.println("options: outgoing, incoming, incomingoutgoing, ..");
    }

    private void incoming(ServerRequest request, ServerResponse response) {
        System.out.println("incoming");
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/pokemon subscribe(java.util.regex.Pattern
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
        public Object processMessage(Session session, HelidonMessage message) {
            System.out.println("TestMessageProcessorIncoming.processMessage session:" + session + " message:" + message);
            //todo insert db row, etc.
            return message + "sent";
        }
    }



    private void outgoing(ServerRequest request, ServerResponse response) {
        System.out.println("outgoing/send message via producer");
        String message = "kafkatest messaging";
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/pokemon subscribe(java.util.regex.Pattern
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
        public Object processMessage(Session session, HelidonMessage message) {
            System.out.println("TestMessageProcessorOutgoing.processMessage session:" + session + " message:" + message);
            //todo insert db row, etc.
            return message + "sent";
        }
    }





    private void incomingoutgoing(ServerRequest request, ServerResponse response) {
        System.out.println("incomingoutgoing...");
        String outgoingmessaging = "kafkatest outgoing messaging resulting/translating from incoming message";
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/pokemon subscribe(java.util.regex.Pattern
                .incoming(new TestMessageProcessorIncomingOutgoing(response, outgoingmessaging)))
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
        public Object processMessage(Session session, HelidonMessage message) {
            System.out.println("TestMessageProcessorIncomingOutgoing.processMessage session:" + session + " message:" + message);
            //todo insert db row, etc.
            messagingClient.channel(exec -> exec
                    .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/pokemon subscribe(java.util.regex.Pattern
                    .outgoing(new TestMessageProcessorOutgoing(), () -> outgoingmessage))
                    .thenAccept(outgoingMessage -> postSendProcessMessage(response, outgoingMessage))
                    .exceptionally(throwable -> sendError(throwable, response));
            response.send("sent message: " + outgoingmessage);

            return message + "sent";
        }
    }





    private void postSendProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("PokemonService.postSendProcessMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }

    private void postRecieveProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("PokemonService.postRecieveProcessMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
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

}
