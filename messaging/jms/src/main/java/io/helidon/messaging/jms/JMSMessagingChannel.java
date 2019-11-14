package io.helidon.messaging.jms;

import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messagingclient.common.InterceptorSupport;
import org.eclipse.microprofile.config.Config;

import javax.jms.*;
import javax.jms.Session;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class JMSMessagingChannel  {
    private final ExecutorService executorService;
    private final ConnectionPool connectionPool;
    private final InterceptorSupport interceptors;
    private final String messagingType = "JMS";
    private final String channelName = "all"; //todo
    private Config config;

    private static final Logger LOGGER = Logger.getLogger(JMSMessagingChannel.class.getName());
//
    public JMSMessagingChannel(ExecutorService executorService, InterceptorSupport interceptors,
                               ConnectionPool connectionPool, Config config) {
        this.executorService = executorService;
        this.interceptors = interceptors;
        this.config = config;
        this.connectionPool = connectionPool;
    }
//
//
//    public Message incoming(
//            IncomingMessagingService messageProcessor, boolean isCloseSession) {
//        LOGGER.fine(() -> String.format("JMSMessagingChannel.channel incoming"));
//        System.out.println("JMSMessagingChannel.doIncoming");
//        return new JMSMessage( //todo get config, msgtype etc from config
//                receiveMessages(messageProcessor, "queue", "text", isCloseSession));
//    }

    //todo currently only processes one, ie doesnt use numberofmessages config
    public Object receiveMessages(IncomingMessagingService testMessageProcessor, String queueName, String messageType, boolean isCloseSession) {
        System.out.println("JMSMessagingChannel.receiveMessages " + " connectionPool:" + connectionPool +
                "queueName:" + queueName + " messageType:" + messageType ) ;
        QueueConnection connection = null;
        javax.jms.Queue queue;
        QueueSession session =null;
        javax.jms.Message msg;
        try {
            connection = connectionPool.connection();
            session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createReceiver(queue);
            //todo allow these to be added
            consumer.setMessageListener(message -> System.out.println( "test message listener message:" + message));
            connection.start();
            System.out.println("--->channel before receive queue:" + queue);
            msg = consumer.receive();
            System.out.println("JMSMessagingChannel.receiveMessages channel msg.getJMSType():" + msg.getJMSType());
            System.out.println("JMSMessagingChannel.receiveMessages channel msg:" + msg);
            System.out.println("JMSMessagingChannel.receiveMessages channel message:" + ((TextMessage)msg).getText()); //todo assumed txt message
            testMessageProcessor.onIncoming(null, null, null); //todo
            System.out.println("JMSMessagingChannel.receiveMessages isCloseSession:" + isCloseSession);
            if (isCloseSession) {
                session.commit();
                session.close();
                connection.close();
            }
            return msg;
        } catch (Exception e) {
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
        return messageType; //todo just returns messageType currently
    }



    public String outgoing(Object existingsession, String queueName, String messageType)  {
        QueueConnection connection = null;
        javax.jms.Queue queue;
        QueueSession session =null;
        javax.jms.Message msg;
        try {
            if (existingsession == null) {
                System.out.println("JMSMessagingChannel.sendMessage existingsession == null");
                connection = connectionPool.connection();
                session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            } else {
                System.out.println("JMSMessagingChannel.sendMessage existingsession:" + existingsession);
                session = (QueueSession)existingsession;
            }
            queue = session.createQueue(queueName);
            String messageTxt = "jms message - messageType:" + messageType;
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
