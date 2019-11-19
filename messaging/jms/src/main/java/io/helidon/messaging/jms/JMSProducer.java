package io.helidon.messaging.jms;

import io.helidon.messaging.Channels;
import io.helidon.messaging.OutgoingMessagingService;
import io.helidon.messaging.ProcessingMessagingService;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsSession;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import javax.jms.*;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class JMSProducer<K, V> implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(JMSProducer.class.getName());
    private ExecutorService externalExecutorService;
    private QueueConnectionFactory queueConnectionFactory; //todo topics
    private String url;
    private String user;
    private String password;
    private String queueName;
    private String channelName;

    public JMSProducer(org.eclipse.microprofile.config.Config config) {
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(ConnectorFactory.OUTGOING_PREFIX)) {
                String striped = propertyName.substring(ConnectorFactory.OUTGOING_PREFIX.length());
                channelName = striped.substring(0, striped.indexOf("."));
                String channelPropertyName = striped.substring(channelName.length() + 1);
                switch (channelPropertyName) {
                    case "url":
                        url = config.getValue(propertyName, String.class);
                        break;
                    case "user":
                        user = config.getValue(propertyName, String.class);
                        break;
                    case "password":
                        password = config.getValue(propertyName, String.class);
                        break;
                    case "queue":
                        queueName = config.getValue(propertyName, String.class);
                        break;
                }
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


    @Override
    public void close() {
    }

    public JMSSubscriberBuilder<K, V> createSubscriberBuilder(V executorService) {
        sendMessage();
        return new JMSSubscriberBuilder<>();
    }

    // todo existingsession for incomingoutgoing processor
    private void sendMessage() {
        QueueConnection connection = null;
        javax.jms.Queue queue;
        ProcessingMessagingService processingMessagingService =
                Channels.getInstance().getProcessingMessagingService(channelName);
        Channels.ProcessingMessagingServiceHolder processingMessagingServiceHolder =
                Channels.getInstance().getProcessingMessagingServiceHolder(channelName);
        OutgoingMessagingService outgoingMessagingService =
                Channels.getInstance().getOutgoingMessagingService(channelName);
        QueueSession session = null;
        try {
            org.eclipse.microprofile.reactive.messaging.Message message;
            if (processingMessagingService != null) {
                session = (QueueSession) processingMessagingServiceHolder.getSession();  //todo topic support
                if (session == null) { //todo currently session should not be null but that is because we assume/require same session in processor case
                    connection = queueConnectionFactory.createQueueConnection();
                    session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
                } else {
                   //processor
                }
                message = processingMessagingService.onIncoming(
                        processingMessagingServiceHolder.getMessage(),
                        processingMessagingServiceHolder.getConnection(), processingMessagingServiceHolder.getSession());
            } else if (outgoingMessagingService != null) {
                connection = queueConnectionFactory.createQueueConnection();
                session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
                message = outgoingMessagingService.onOutgoing(((AQjmsSession) session).getDBConnection() , session);
            } else
                throw new Exception("no OutgoingMessagingService or ProcessingMessagingService found for channelName:" + channelName);
            queue = session.createQueue(queueName);
            System.out.println("JMSProducer channel before send queueName:" + queue);
            Message unwrappedJMSMessage = (Message) message.unwrap(Message.class);
            session.createSender(queue).send(unwrappedJMSMessage);
            session.commit();
            System.out.println("--->JMSProducer sendMessage committed messageTxt:" + unwrappedJMSMessage + " on queueName:" + queue);
            session.close();
            if (connection != null) connection.close(); //todo should be keeping or passing the conn in to do close
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
        }
    }
}
