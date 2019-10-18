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
import io.helidon.messagingclient.Message;
import io.helidon.messagingclient.MessagingClient;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;


public class PokemonService implements Service {

    /**
     * Local logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(PokemonService.class.getName());

    private final MessagingClient messagingClient;

    PokemonService(MessagingClient dbClient) {
        this.messagingClient = dbClient;
    }

    private void log(String message) {
        System.out.println("PokemonService.log message:" + message);
    }

    @Override
    public void update(Routing.Rules rules) {
        log("update");
        rules.get("/", this::listenForMessagesGET)
                .post("/", Handler.create(Pokemon.class, this::listenForMessages))
                .get("/{name}", this::sendMessages);
    }


    private void listenForMessagesGET(ServerRequest request, ServerResponse response) {
        log("listenForMessagesGET");
        listenForMessages(request, response, null);

    }

    private void listenForMessages(ServerRequest request, ServerResponse response, Pokemon pokemon) {
        log("listenForMessages");
        messagingClient.listenForMessages(exec -> exec
                .createNamedFilter("kafkasubscribewithpattern")  //  subscribe(java.util.regex.Pattern
//  if pokemon is !null ...              .namedParam(pokemon)
                .execute())
                .thenAccept(messageReceived -> processMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
    }


    private void processMessage(ServerResponse response, Message messageReceived) {
        System.out.println("PokemonService.processMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }

    private void sendMessages(ServerRequest request, ServerResponse response) {
        log("getP sendMessages/send message via producer");
        String message = "test messaging";
        messagingClient.sendMessages(message);
        response.send(" message sent:" + message);
    }


    private void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
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
