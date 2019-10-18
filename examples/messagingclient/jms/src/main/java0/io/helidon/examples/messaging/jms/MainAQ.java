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

package io.helidon.examples.db.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;

import io.helidon.config.Config;
import io.helidon.messaging.Listener;
import io.helidon.messaging.MessagingServer;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;



import javax.jms.*;
import javax.jms.Queue;
import javax.sql.DataSource;



/**
 * Simple Hello World rest application.
 */
public final class MainAQ {

    /**
     * Cannot be instantiated.
     */
    private MainAQ() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     * @throws java.io.IOException if there are problems reading logging properties
     */
    public static void main(final String[] args) throws IOException {
        startServer();
    }

    static WebServer startServer0() throws IOException {
        // load logging configuration
        LogManager.getLogManager().readConfiguration(
                MainAQ.class.getResourceAsStream("/logging.properties"));

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        // Get webserver config from the "server" section of application.yaml
        ServerConfiguration serverConfig =
                ServerConfiguration.builder(config.get("server"))
                        .tracer(TracerBuilder.create(config.get("tracing")).buildAndRegister())
                        .build();

        WebServer server = WebServer.create(serverConfig, createRouting(config));

        // Start the server and print some info.
        server.start().thenAccept(ws -> {
            System.out.println(
                    "WEB server is up! http://localhost:" + ws.port() + "/");
        });

        // Server threads are not daemon. NO need to block. Just react.
        server.whenShutdown().thenRun(()
                -> System.out.println("WEB server is DOWN. Good bye!"));

        return server;
    }

    /**
     * Messaging messaging = Messaging.create(config.get("messaging"));
     * AqMessaging messaging = AqMessaging.create(config.get("aq"));
     * messaging.sendMessages((session, message) -> processMessage(session, message));
     * <p>
     * public interface Session {
     * SendingMessage createMessage();
     * send(Message message);
     * }
     * <p>
     * public static interface SendingMessage {
     * addHeader(String, String);
     * writeContent(Object anObject);
     * }
     * public static interface Message {
     * Map<String, Object> headers();
     * T as(Class<T> type);
     * T as(GenericType<T> type);
     * }
     */

    public static class AqMessaging { //todo  this would be in

//        public static AqMessaging create(ServerConfiguration config) {
//            return null;
//        }

        public static AqMessaging create(Config config, Object listeners) {
            Config url = config.get("url");
            Config username = config.get("username");
            Config password = config.get("password");
            System.out.println("AqMessaging.create " +
                    "\nurl:" + url +
                    "\nusername:" + username +
                    "\npassword:" + password + " \n");
            //todo create the session and listeners here
            return null;
        }

        public void addListener(Object o, Object o1) {
        }
    }

//    public interface Session {
//        SendingMessage createMessage();
//
//        void send(Message message);
//    }

    public static interface SendingMessage {
        void addHeader(String string, String string2);

        void writeContent(Object anObject);
    }

    public static interface Message {
        Map<String, Object> headers();
//        T as(Class<T> type);
//        T as(GenericType<T> type);
    }


    interface ConnectionWork {
//        void doWork(java.sql.Connection connection);

        // get conn from aqsession
        Object processMessage(java.sql.Connection connection);
    }

    static AqMessaging startServer() throws IOException {
        // load logging configuration
        LogManager.getLogManager().readConfiguration(
                MainAQ.class.getResourceAsStream("/logging.properties"));

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        //todo, this would be/in new io.helidon.messaging instead of io.helidon.webserver
        ServerConfiguration serverConfig =
                ServerConfiguration.builder(config.get("server"))
                        .tracer(TracerBuilder.create(config.get("tracing")).buildAndRegister())
                        .build();

        MessagingServer messagingServerserver =
                MessagingServer.create(serverConfig, createListener(config));

        Config server = config.get("server");

        //todo allow add of listeners here during create and afterward as well
        AqMessaging messaging = AqMessaging.create(server, null); //second arg is createListeners(config) like createRouting(config)
        if (messaging != null) {
            messaging.addListener(null, null);
//            ((AQjmsSession) session).getDBConnection();
//            messaging.sendMessages((session, message) -> processMessage(session, message));
//            ((AQjmsSession) session).getDBConnection();
            /**  ConnectionWork connwork = new ConnectionWork() {
             public void doWork(Connection connection) {

             }

             public Object processMessage(Connection connection) {
             return null;
             }
             }; */
            ConnectionWork connectionWork = (java.sql.Connection connection) -> processMessage(connection);

            connectionWork.processMessage(connection);
//            messaging.sendMessages(
//                    (java.sql.Connection connection) -> processMessage(connection)
//            );
        }

        return null; // server;

        // Get webserver config from the "server" section of application.yaml
//        ServerConfiguration serverConfig =
//                ServerConfiguration.builder(config.get("server"))
//                        .tracer(TracerBuilder.create(config.get("tracing")).buildAndRegister())
//                        .build();

//        WebServer server = WebServer.create(serverConfig, createRouting(config));

        // Start the server and print some info.
//        server.start().thenAccept(ws -> {
//            System.out.println(
//                    "WEB server is up! http://localhost:" + ws.port() + "/");
//        });

        // Server threads are not daemon. NO need to block. Just react.
//        server.whenShutdown().thenRun(()
//                -> System.out.println("WEB server is DOWN. Good bye!"));

//        return server;
    }

    private static Object processMessage(Connection connection) {
        return null;
    }


//    private static Object processMessage(Connection connection) {
//        return null;
//    }


    /**
     * Creates new {@link io.helidon.messaging.Listener}.
     *
     * @param config configuration of this server
     * @return listener configured with JSON support, a health check, and a service
     */
    private static Listener createListener(Config config) {
//        Config dbConfig = config.get("db");
        Config messagingConfig = config.get("messaging");

        // Interceptors are added through a service loader - see mongoMessagingexample for explicit interceptors
//        DbClient dbClient = DbClient.create(dbConfig);
//
//        HealthSupport health = HealthSupport.builder()
//                .add(DbClientHealthCheck.create(dbClient))
//                .build();
//
//        return Routing.builder()
//                .register(JsonSupport.create())
//                .register(JsonBindingSupport.create())
//                .register(DbResultSupport.create())
//                .register(health)                   // Health at "/health"
//                .register(MetricsSupport.create())  // Metrics at "/metrics"
//                .register("/db", new PokemonService(dbClient))
//                .build();
        return null;
    }

    /**
     * import oracle.jms.*;
     * <p>
     * import javax.jms.*;
     * import javax.jms.Queue;
     * <p>
     * import oracle.jms.pool.OracleDataSource;
     * import oracle.AQ.AQException;
     * import oracle.AQ.AQQueueTable;
     * import oracle.AQ.AQQueueTableProperty;
     */
    javax.jms.Queue queue;
    QueueSession session;

    class MessageListener implements Runnable {
        public void run() {
            while (true) {
                try {
                    System.out.println("InventoryService.listenForMessages:" + dolistenForMessages());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String dolistenForMessages() throws Exception {
            QueueConnection connection = null;
            QueueConnectionFactory q_cf = AQjmsFactory.getQueueConnectionFactory(getAQDataSource());
            connection = q_cf.createQueueConnection();
            session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            queue = ((AQjmsSession) session).getQueue(queueOwner, queueName);
            java.sql.Connection dbConnection = ((AQjmsSession) session).getDBConnection();
            MessageConsumer consumer = session.createConsumer(queue);
            //            MessageConsumer consumer = session.createConsumer(queue, "service = order");
            connection.start();
            System.out.println("-----> InventoryService.listenForMessages before receive queue:" + queue);
            MapMessage mapMsg = (MapMessage) consumer.receive();
            System.out.println("InventoryService.listenForMessages mapMsg properties...");
            Enumeration propertyNames = mapMsg.getPropertyNames();
            Map<String, String> traceheadersFromMsg = new HashMap();
            while (propertyNames.hasMoreElements()) {
                Object propertyname = propertyNames.nextElement();
                System.out.println("InventoryService.listenForMessages propertyname:" +
                        propertyname);
// for (String tracingHeaderKey : tracingHeaderKeys) {
// if (propertyname.equals(tracingHeaderKey)) {
// String stringProperty = mapMsg.getStringProperty(tracingHeaderKey);
// System.out.println("InventoryService.listenForMessages adding value:" + stringProperty);
// traceheadersFromMsg.put(tracingHeaderKey, stringProperty);
// }
            }
            String orderid = mapMsg.getStringProperty("orderid");
            System.out.println("-----> Inventory inventory status received orderid:" + orderid);
            String action = mapMsg.getStringProperty("action");
            System.out.println("-----> Inventory action received:" + action);
            String service = mapMsg.getStringProperty("service");
            System.out.println("-----> Inventory service received:" + service);
            if (action == null) return "action was null";
            switch (action) {
                case "something":
                    System.out.println("MessageListener.dolistenForMessages something");
                    break;
                default:
                    System.out.println("-----> Inventory action default:" + action);
                    break;
            }
            //todo eventually... dbConnection. listenForMessages update order status to success or fail
            //                if (!"order".equals(service)) {
            //                    //todo message selector although rollback is interesting to show..
            //                    System.out.println("-----> Inventory mapMsg.getString(\"service\") is not order type:" +
            //                            service + " rolling back and returning...");
            //                    session.rollback();
            //                    return "rollback";
            //                }
            session.commit();
            session.close();
            connection.close();
            return "success";
        }

    }


    private DataSource getAQDataSource() {
        return null;
    }

    private static final String queueOwner = "aquser1";
    private static final String queueOwnerPW = "AQWelcome123###";
    private static final String queueName = "datademoqueue";
    private static final String queueNameInventory = "inventoryqueue";
    private String publishEventToBroker(String eventType, String eventValue) {
        Session session = null;
        try {
            System.out.println("----->  insertOrderAndSendMessage enter");
//            OracleDataSource aqDataSource = getAQDataSource();
            DataSource aqDataSource = getAQDataSource();
            QueueConnectionFactory q_cf = AQjmsFactory.getQueueConnectionFactory(aqDataSource);
            QueueConnection q_conn = q_cf.createQueueConnection();
            session = q_conn.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            Connection dbConnection = ((AQjmsSession) session).getDBConnection();
            System.out.println("----->  insertOrderAndSendMessage dbConnection:" + dbConnection);
            //            dbConnection.createStatement().executeUpdate(
            //                    "insert into orders values ('" + orderid + "', '" + inventoryid + "', " + ordercount + ", '" + status + "') ");
            Queue queue = ((AQjmsSession) session).getQueue(queueOwner, queueNameInventory);
            ((AQjmsDestination) queue).start(session, true, true);
            QueueSender sender = ((AQjmsSession) session).createSender(queue);
            MapMessage msg = session.createMapMessage();
            msg.setStringProperty("service", "inventory");
            msg.setStringProperty("orderid", eventValue);
            msg.setStringProperty("action", eventType);
            msg.setStringProperty("x-request-id", "testx-request-id");
            sender.send(msg);
            session.commit();
            System.out.println("----->  publishEventToBroker " +
                    "eventType:" + eventType + " eventValue:" + eventValue + " on queue:" + queue);
            session.close();
            //     q_conn.close();
            return "" + queue;
        } catch (Exception e) {
            System.out.println("----->  insertOrderAndSendMessage failed " +
                    "(will attempt rollback if session is not null):" + e + " session:" + session);
            e.printStackTrace();
            if (session != null) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    System.out.println("----->  insertOrderAndSendMessage session.rollback() failed:" + e1);
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }


}