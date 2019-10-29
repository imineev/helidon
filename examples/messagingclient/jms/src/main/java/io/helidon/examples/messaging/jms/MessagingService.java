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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;
import io.helidon.messagingclient.*;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import oracle.jms.AQjmsSession;


public class MessagingService implements Service {

    private static final Logger LOGGER = Logger.getLogger(MessagingService.class.getName());

    private final MessagingClient messagingClient;
    private boolean isCompensate;

    MessagingService(MessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::noop)
//                .post("/", Handler.create(MyFilter.class, this::listenForMessagesPOST))
                .get("/outgoing", this::outgoing)
                .get("/incoming", this::incoming)
                .get("/incomingoutgoing", this::incomingoutgoing)
                .get("/booktravel", this::booktravel)
                .get("/reserve", this::incomingoutgoingreserve)
                .get("/compensate", this::incomingoutgoingcompensate);
    }


    private void noop(ServerRequest request, ServerResponse response) {
        System.out.println("options: outgoing, incoming, incomingoutgoing, ..");
    }

    private void incoming(ServerRequest request, ServerResponse response) {
        System.out.println("incoming");
        String filter = "jmssubscribewithpattern";
        doIncoming(response, filter, null, null, sagaId);
    }

    private String doIncoming(ServerResponse response, String filter, String queue, String message, String sagaId) {
        messagingClient.channel(exec -> exec
                .filterForEndpoint(filter)//todo get from config/param subscribe(java.util.regex.Pattern
                .incoming(new TestMessageProcessorIncoming()))
                .thenAccept(messageReceived -> postRecieveProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
        return "fail"; // get message from TestMessageProcessorIncoming or above
    }

    class TestMessageProcessorIncoming implements MessageProcessor {
        @Override
        public Object processMessage(HelidonMessage message) {
            return processMessage(null, message);
        }

        @Override
        public Object processMessage(Object session, HelidonMessage message) {
            System.out.println("TestMessageProcessorIncoming.processMessage session:" + session + " message:" + message);
            try {
                System.out.println("TestMessageProcessorIncoming.processMessage ((AQjmsSession) session).getDBConnection():" +
                        ((AQjmsSession) session).getDBConnection());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //todo insert db row, etc.
            return message + "sent";
        }
    }

    private void postRecieveProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("MessagingService.postRecieveProcessMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }


    private void outgoing(ServerRequest request, ServerResponse response) {
        System.out.println("outgoing/send message via producer");
        String message = "jmstest messaging";
        doOutgoing(response, message, null, sagaId);
    }

    private void doOutgoing(ServerResponse response, String message, String queue, String sagaId) {
        messagingClient.channel(exec -> exec
                .filterForEndpoint("jmssubscribewithpattern")//todo get from config/param subscribe(java.util.regex.Pattern
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
            System.out.println("TestMessageProcessorOutgoing.processMessage session:" + session + " message:" + message);
            //todo insert db row, etc.
            return message + "sent";
        }
    }

    private void postSendProcessMessage(ServerResponse response, HelidonMessage messageReceived) {
        System.out.println("MessagingService.postSendProcessMessage messageReceived.getString():" + messageReceived.getString());
        response.send("received message: " + messageReceived.getString());
    }


    private String booktravel(ServerRequest request, ServerResponse response) {
        return receiveTripBookingRequest();
    }

    String bookingstate = "unknown";
    String sagaId;
    boolean isAutocompensating;/// 0. show plain java without 1. emulate escrow and emulate saga api 2. actual escrow emulate saga 3. show everything
    boolean isCompensationTest;

    String receiveTripBookingRequest() {
        try {
            beginSaga(); //no need to write to saga table
            bookEventtTickets();
            bookFlight();
            bookHotel();
            commitSaga();
            bookingstate = "success";
        } catch (Exception exception) {
            bookingstate = "fail";
            if (!isAutocompensating) abortSaga();
        }
        return bookingstate;
    }

    private void beginSaga() {
        sagaId = "testsagaid1";
    }

    private void bookEventtTickets() throws Exception {
        doOutgoing(null, "book", "eventticketingqueue", sagaId);
        if (!Boolean.valueOf(
                doIncoming(null, null, "eventticketingqueue", "bookingstatus", sagaId))) {
            throw new Exception("booking failed for tickets");
        }
    }

    private void bookFlight() throws Exception {
        doOutgoing(null, "book", "flightqueue", sagaId);
        if (!Boolean.valueOf(
                doIncoming(null, null, "flightqueue", "bookingstatus", sagaId))) {
            throw new Exception("booking failed for flight");
        }
    }

    private void bookHotel() throws Exception {
        doOutgoing(null, isCompensationTest ? "bookfail" : "book", "hotelqueue", sagaId);
        if (!Boolean.valueOf(
                doIncoming(null, null, "hotelqueue", "bookingstatus", sagaId))) {
            throw new Exception("booking failed for hotel");
        }
    }

    private void commitSaga() {
        if (!isAutocompensating) {
            doOutgoing(null, "commit", "eventticketingqueue", sagaId);
            doOutgoing(null, "commit", "flightqueue", sagaId);
            doOutgoing(null, "commit", "hotelqueue", sagaId);
        } else {
            //call commit sproc
        }
    }

    private void abortSaga() {
        if (!isAutocompensating) {
            doOutgoing(null, "compensate", "eventticketingqueue", sagaId);
            doOutgoing(null, "compensate", "flightqueue", sagaId);
            doOutgoing(null, "compensate", "hotelqueue", sagaId);
        }
    }


    //todo this would be based on the message action not the rest call url...
    //reduce inventory amount for given service and send
    private void incomingoutgoingreserve(ServerRequest request, ServerResponse response) {
        isCompensate = false;
        incomingoutgoing(request, response);
    }

    private void incomingoutgoingreservefail(ServerRequest request, ServerResponse response) {
        isCompensate = false;
        incomingoutgoing(request, response);
    }

    //todo this would be based on the message action not the rest call url...
    private void incomingoutgoingcompensate(ServerRequest request, ServerResponse response) {
        isCompensate = true;
        incomingoutgoing(request, response);
    }

    // messaging.addListener((session, message) -> processMessage(session, message));
    private void incomingoutgoing(ServerRequest request, ServerResponse response) {
        System.out.println("incomingoutgoing...");
        String outgoingmessaging = "jmstest outgoing messaging resulting/translating from incoming message";
        messagingClient.channel(exec -> exec // messagechanneloptions
                .filterForEndpoint("jmssubscribewithpattern")// messagechannel
                // CompletionStage<HelidonMessage> ...
                .incoming(new TestMessageProcessorIncomingOutgoing(response, outgoingmessaging), false))
//                .incoming(message -> processMessage(message))
                .thenAccept(messageReceived -> postRecieveProcessMessage(response, messageReceived))
                .exceptionally(throwable -> sendError(throwable, response));
        response.send("message received and message sent:" + outgoingmessaging);
    }

//    <D extends MessagingChannel<D, R>, R> MessagingChannel<D,R> filterForEndpoint(String filter);
//    <D extends MessagingChannel<D, R>, R> MessagingChannel<D,R> processMessage(String filter) {
//        return null;
//    }
//    private static void  processMessage(CompletionStage<HelidonMessage> message) {
//    private static void  processMessage(Function<String, T>) {
//    }


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
            System.out.println("TestMessageProcessorIncomingOutgoing.processMessage session:" + session + " message:" + message);
            try {
                Connection dbConnection = ((AQjmsSession) session).getDBConnection();
                System.out.println("TestMessageProcessorIncomingOutgoing.processMessage ((AQjmsSession) session).getDBConnection():" +
                        dbConnection);
                String tablename = "ticketing"; //or hotel or airline (eventname, hotelname, airlinename) inventorycount
                //todo mod dbclient to use existing connection and use it here...
//                dbConnection.createStatement().execute("insert into " + tablename + "  values('olympics', 10)");
//                System.out.println("TestMessageProcessorIncomingOutgoing.processMessage ticketing inserted");
                dbConnection.createStatement().execute("UPDATE " + tablename +
                        " SET inventorycount = inventorycount " + (isCompensate ? "-" : "+") + " 1");
                System.out.println("TestMessageProcessorIncomingOutgoing.processMessage ticketing updated");
                ResultSet rs = dbConnection.createStatement().executeQuery("select * from " + tablename);
                while (rs.next()) {
                    int inventorycount = rs.getInt("inventorycount");
                    System.out.println("TestMessageProcessorIncomingOutgoing.processMessage inventorycount:" + inventorycount);
                }
//                dbConnection.createStatement().execute("create table ticketing (eventname varchar(64),  inventorycount integer)");

            } catch (Exception e) {
                e.printStackTrace();
            }
            messagingClient.channel(exec -> exec
                    .filterForEndpoint("jmssubscribewithpattern")//todo get from config or param subscribe(java.util.regex.Pattern
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

}
