package io.helidon.messaging.jms;

import io.helidon.config.Config;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.jms.AQjmsQueueConnectionFactory;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import javax.jms.*;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class JMSProducer<K, V> implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(JMSProducer.class.getName());
    private ExecutorService externalExecutorService;
    private QueueConnectionFactory queueConnectionFactory; //todo topics
    private String url;
    private String user;
    private String password;

    public JMSProducer(org.eclipse.microprofile.config.Config config) {
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(ConnectorFactory.OUTGOING_PREFIX)) {
                String striped = propertyName.substring(ConnectorFactory.OUTGOING_PREFIX.length());
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
            System.out.println("JMSProducer.createConnectionFactory queueConnectionFactory:" + queueConnectionFactory +
                    " url:" + url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void close() {
        //close JMS session
    }

    public JMSSubscriberBuilder<K, V> createSubscriberBuilder(V executorService) {
//        return new SimpleSubscriberBuilder;

//        this.externalExecutorService = executorService;
        System.out.println("KafkaProducer.createSubscriberBuilder");
        //todo this is incorrect/incomplete, would be sent via SubscriberBuilder/CompletionSubscriber using OutgoingMessagingService etc
        sendMessage(null, "demoqueue", "testmessage");
        return new JMSSubscriberBuilder<>(); //todo...

    }

    // todo existingsession for incomingoutgoing processor
    public String sendMessage(Object existingsession, String queueName, String messageTxt)  {
        QueueConnection connection = null;
        javax.jms.Queue queue;
        QueueSession session =null;
        javax.jms.Message msg;
        try {
            if (existingsession == null) {
                System.out.println("JMSMessagingChannel.sendMessage existingsession == null");
                connection = queueConnectionFactory.createQueueConnection();
                session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            } else {
                System.out.println("JMSMessagingChannel.sendMessage existingsession:" + existingsession);
                session = (QueueSession)existingsession;
            }
            queue = session.createQueue(queueName);
            System.out.println("--->channel before send queue:" + queue);
            session.createSender(queue).send(session.createTextMessage(messageTxt));
            session.commit();
            System.out.println("sendMessage committed messageTxt:" + messageTxt + " on queue:" + queue);
            session.close();
            if(connection !=null) connection.close(); //todo should be keeping or passing the conn in to do close
            return "test"; // this is null ... queue.toString();
        } catch (Exception e) {
            System.out.println("sendMessage failed " +
                    "(will attempt rollback if session is not null):" + e + " session:" + session);
            e.printStackTrace();
            if (session != null) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    System.out.println("sendMessage session.rollback() failed:" + e1);
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }
}
