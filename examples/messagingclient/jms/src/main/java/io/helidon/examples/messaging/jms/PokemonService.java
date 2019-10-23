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

import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;
import io.helidon.messagingclient.*;
import io.helidon.webserver.Handler;
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
        rules.get("/", this::listenForMessagesGET)
                .post("/", Handler.create(Pokemon.class, this::listenForMessages))
                .get("/{name}", this::sendMessages);
    }

    private void listenForMessagesGET(ServerRequest request, ServerResponse response) {
        System.out.println("listenForMessagesGET");
        listenForMessages(request, response, null);
    }

    //
    private void listenForMessages(ServerRequest request, ServerResponse response, Pokemon pokemon) {
        System.out.println("listenForMessages");
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/pokemon subscribe(java.util.regex.Pattern
                .incoming(new TestMessageProcessorIncoming()))
                .thenAccept(messageReceived -> postProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    class TestMessageProcessorIncoming implements MessageProcessor {
        @Override
        public Object processMessage(HelidonMessage message) {
            return processMessage(null, message);
        }

        @Override
        public Object processMessage(Session session, HelidonMessage message) {
            System.out.println("TestMessageProcessor.processMessage session:" + session + " message:" + message);
            //todo insert db row, etc.
            return message + "sent";
        }
    }

    private void postProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("PokemonService.processMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }

    private void sendMessages(ServerRequest request, ServerResponse response) {
        System.out.println("sendMessages/send message via producer");
        String message = "test messaging";
        messagingClient.channel(exec -> exec
                .filterForEndpoint("kafkasubscribewithpattern")//todo get from config/pokemon subscribe(java.util.regex.Pattern
                .outgoing(new TestMessageProcessorOutgoing(), () -> "jms test message"))
                .thenAccept(messageReceived -> postProcessMessage(response, messageReceived))
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
            System.out.println("TestMessageProcessor.processMessage session:" + session + " message:" + message);
            //todo insert db row, etc.
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

}
