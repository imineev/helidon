package io.helidon.messaging.jms;

import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.jms.connector.JMSPublisherBuilder;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsSession;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import javax.jms.*;
import java.io.Closeable;
import java.sql.SQLException;
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

    public JMSConsumer(org.eclipse.microprofile.config.Config config) {
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(ConnectorFactory.INCOMING_PREFIX)) {
                String striped = propertyName.substring(ConnectorFactory.INCOMING_PREFIX.length());
                String channelName = striped.substring(0, striped.indexOf("."));
                String channelPropertyName = striped.substring(channelName.length() + 1);
                if (channelPropertyName.equals("url")) url = config.getValue(propertyName, String.class);
                else if (channelPropertyName.equals("user")) user = config.getValue(propertyName, String.class);
                else if (channelPropertyName.equals("password")) password = config.getValue(propertyName, String.class);

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
            System.out.println("JMSConsumer.createConnectionFactory queueConnectionFactory:" + queueConnectionFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    //    public JMSPublisherBuilder createPublisherBuilder(ExecutorService executorService) {
    public JMSPublisherBuilder<K, V> createPublisherBuilder(ExecutorService executorService) {
        this.externalExecutorService = executorService;
        System.out.println("JMSConsumer.createPublisherBuilder");
        return new JMSPublisherBuilder<>(subscriber -> {
            externalExecutorService.submit(() -> {
                try {
                    System.out.println("JMSConsumer.listening for messages...");
//                    while (!closed.get()) {
                    while (true) {
                        boolean isCloseSession = false;
                        QueueConnection connection = null;
                        javax.jms.Queue queue;
                        QueueSession session = null;
                        javax.jms.Message msg;
                        try {
                            System.out.println("JMSConsumer.createPublisherBuilder before queueconnection");
                            connection = queueConnectionFactory.createQueueConnection();
                            System.out.println("JMSConsumer.createPublisherBuilder queueconnection:" + connection);
                            session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
                            queue = session.createQueue(getQueueName());
                            java.sql.Connection dbConnection = ((AQjmsSession) session).getDBConnection();
                            System.out.println("JMSConsumer.createPublisherBuilder dbConnection:" + dbConnection);
                            MessageConsumer consumer = session.createReceiver(queue);
                            //todo allow listeners, filters, etc. to be added
                            consumer.setMessageListener(message -> System.out.println("test message listener message:" + message));
                            connection.start();
                            System.out.println("--->channel before receive queue:" + queue);
                            msg = consumer.receive();
                            System.out.println("JMSMessagingChannel.receiveMessages channel msg.getJMSType():" + msg.getJMSType());
                            System.out.println("JMSMessagingChannel.receiveMessages channel msg:" + msg);
                            System.out.println("JMSMessagingChannel.receiveMessages channel message:" + ((TextMessage) msg).getText()); //todo assumed txt message
//
                            MessageWithConnectionAndSession<K,V> messageWithConnectionAndSession =
                                    new MessageWithConnectionAndSession<>
                                            (msg, dbConnection, session, null, null);
                            subscriber.onNext(messageWithConnectionAndSession);
                            System.out.println("JMSMessagingChannel.receiveMessages isCloseSession:" + isCloseSession);
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

    private String getQueueName() {
        return "demoqueue"; //todo
    }


    @Override
    public void close() {
        this.closed.set(true);
    }



}
