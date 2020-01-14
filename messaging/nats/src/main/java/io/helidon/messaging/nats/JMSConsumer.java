package io.helidon.messaging.jms;

import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.jms.connector.JMSPublisherBuilder;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsSession;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import javax.jms.*;
import java.io.Closeable;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class JMSConsumer<K, V> implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(JMSConsumer.class.getName());
    private AtomicBoolean closed = new AtomicBoolean(false);
    private ExecutorService externalExecutorService;
    private QueueConnectionFactory queueConnectionFactory; //todo topics
    private String url;
    private String user;
    private String password;
    private String queueName;
    private String selector;
    String channelName;

    public JMSConsumer(org.eclipse.microprofile.config.Config config) {
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(ConnectorFactory.INCOMING_PREFIX)) {
                String striped = propertyName.substring(ConnectorFactory.INCOMING_PREFIX.length());
                channelName = striped.substring(0, striped.indexOf("."));
                String channelPropertyName = striped.substring(channelName.length() + 1);
                if (channelPropertyName.equals("url")) url = config.getValue(propertyName, String.class);
                else if (channelPropertyName.equals("user")) user = config.getValue(propertyName, String.class);
                else if (channelPropertyName.equals("password")) password = config.getValue(propertyName, String.class);
                else if (channelPropertyName.equals("queue")) queueName = config.getValue(propertyName, String.class);
                else if (channelPropertyName.equals("selector")) selector = config.getValue(propertyName, String.class);

            }
        }
        createConnectionFactory(); // todo move this out
    }

    private void createConnectionFactory() {

        try {
            OracleDataSource oracleDataSource = new oracle.jdbc.pool.OracleDataSource();
            oracleDataSource.setURL(url);
            oracleDataSource.setUser(user);
            oracleDataSource.setPassword(password);
            queueConnectionFactory = new AQjmsQueueConnectionFactory();
            ((AQjmsQueueConnectionFactory) queueConnectionFactory).setDatasource(oracleDataSource);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public JMSPublisherBuilder<K, V> createPublisherBuilder(ExecutorService executorService) {
        this.externalExecutorService = executorService;
        return new JMSPublisherBuilder<>(subscriber -> {
            externalExecutorService.submit(() -> {
                try {
                    while (true) { //todo config for number of msg/requests made
                        boolean isCloseSession = MessagingClient.isIncoming; //todo false for processor true for incoming
                        QueueConnection connection = null;
                        javax.jms.Queue queue;
                        QueueSession session = null;
                        javax.jms.Message msg;
                        try {
                            connection = queueConnectionFactory.createQueueConnection();
                            session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
                            queue = session.createQueue(queueName);
                            java.sql.Connection dbConnection = ((AQjmsSession) session).getDBConnection();
                            MessageConsumer consumer = selector == null?
                                    session.createConsumer(queue):
                                    session.createConsumer(queue, selector);
//                            MessageConsumer consumer = session.createReceiver(queue); // for msg enqueued by Non-JMS (PL/SQL client)
                            connection.start();
                            System.out.println("JMSConsumer queue:" + queueName + " before receive...");
                            msg = consumer.receive();
                            Enumeration<String> e = (Enumeration<String>) msg.getPropertyNames();
                            String msgPropertyDebugString = "";
                            while (e.hasMoreElements()) {
                                String name = e.nextElement();
                                msgPropertyDebugString += name + "=" + msg.getStringProperty(name) + (e.hasMoreElements()?" , ":"");
                            }
                            System.out.println("JMSConsumer message received:" + msg +
                                    " on queueName:" + queue + " properties:" + msgPropertyDebugString);
                            MessageWithConnectionAndSession<K,V> messageWithConnectionAndSession =
                                    new MessageWithConnectionAndSession<>
                                            (channelName, msg, dbConnection, session, null, null);
                            subscriber.onNext(messageWithConnectionAndSession);
                            if (isCloseSession) {
                                session.commit();
                                session.close();
                                connection.close();
                            }
                            Thread.sleep(1000);
                        }  catch (Exception e) {
                            e.printStackTrace();
                            try {
                                if (session != null) session.rollback();
                            } catch (JMSException e1) {
                                e1.printStackTrace();
                            }
                        } finally {
                            if (isCloseSession) {
                                try {
                                    if (session != null) {
                                        session.close();
                                    }
                                    if (connection != null) {
                                        connection.close();
                                    }
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                } catch (Exception ex) {
                    if (!closed.get()) {
                        throw ex;
                    }
                }
            });
        });
    }

    @Override
    public void close() {
        this.closed.set(true);
    }



}
