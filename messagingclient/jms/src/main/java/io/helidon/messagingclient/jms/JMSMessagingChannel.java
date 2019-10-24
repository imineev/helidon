package io.helidon.messagingclient.jms;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.messagingclient.*;
import io.helidon.messagingclient.HelidonMessage;
import io.helidon.messagingclient.common.InterceptorSupport;
import javax.jms.*;
import javax.jms.Session;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class JMSMessagingChannel implements MessagingChannel {
    private final ExecutorService executorService;
    private final ConnectionPool connectionPool;
    private final JMSMessagingClientConfig config;
    private final InterceptorSupport interceptors;
    private final String messagingType = "JMS";
    private final String channelName = "all"; //todo
    private final MessagingChannelType channelType = MessagingChannelType.MESSAGING;

    private static final Logger LOGGER = Logger.getLogger(JMSMessagingChannel.class.getName());
//
    public JMSMessagingChannel(ExecutorService executorService, InterceptorSupport interceptors,
                               ConnectionPool connectionPool, JMSMessagingClientConfig config) {
        this.executorService = executorService;
        this.interceptors = interceptors;
        this.config = config;
        this.connectionPool = connectionPool;
    }


    public CompletionStage<HelidonMessage> incoming(MessageProcessor messageProcessor) {
        return incoming(messageProcessor, true);
    }

    public CompletionStage<HelidonMessage> incoming(
            MessageProcessor messageProcessor, boolean isCloseSession) {
        LOGGER.fine(() -> String.format("JMSMessagingChannel.channel incoming"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingInterceptorContextContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingInterceptorContextContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingInterceptorContextContext);
        System.out.println("JMSMessagingChannel.doIncoming");
        // query and channel future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            channelFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        return messagingContextFuture.thenCompose(messagingContext -> {
            executorService.submit(() -> {
                channelFuture.complete(null);
                queryFuture.complete(
                        new JMSMessage(receiveMessages(messageProcessor, config.queue(), config.messagetype(), isCloseSession)));
            });
            return queryFuture;
        });
    }

    //todo currently only processes one, ie doesnt use numberofmessages config
    public Object receiveMessages(MessageProcessor testMessageProcessor, String queueName, String messageType, boolean isCloseSession) {
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
            testMessageProcessor.processMessage(session, new HelidonMessage() {
                @Override
                public String getString() {
                    return msg.toString();
                }
            });
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



    @Override
    public CompletionStage<HelidonMessage> outgoing(
            MessageProcessor messageProcessor, HelidonMessage message) {
        return outgoing(messageProcessor, message, null, null);
    }

    @Override
    public CompletionStage<HelidonMessage> outgoing(
            MessageProcessor messageProcessor, HelidonMessage message, Object session, String queueName) {
        LOGGER.fine(() -> String.format("JMSMessagingChannel.channel outgoing"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingInterceptorContextContext =
                MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingInterceptorContextContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture =
                invokeInterceptors(messagingInterceptorContextContext);
        // query and channel future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            channelFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        return messagingContextFuture.thenCompose(messagingContext -> {
            executorService.submit(() -> {
                channelFuture.complete(null);
                queryFuture.complete(
                        new JMSMessage(sendMessage(session, queueName!=null?queueName:config.queue(), config.messagetype())));
            });
            return queryFuture;
        });
    }

    public String sendMessage(Object existingsession, String queueName, String messageType)  {
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
    /**
     * Invoke all interceptors.
     *
     * @param messagingContext initial interceptor context
     * @return future with the result of interceptors processing
     */
    CompletionStage<MessagingInterceptorContext> invokeInterceptors(MessagingInterceptorContext messagingContext) {
        CompletableFuture<MessagingInterceptorContext> result = CompletableFuture.completedFuture(messagingContext);

        messagingContext.context(Contexts.context().orElseGet(Context::create));

        for (MessagingInterceptor interceptor : interceptors.interceptors(channelType(), channelName())) {
            result = result.thenCompose(interceptor::channel);
        }

        return result;
    }

    private MessagingChannelType channelType() {
        return channelType;
    }

    private String channelName() {
        return channelName;
    }


    /**
     * Update the interceptor context with the channel name, channel and
     * channel parameters.
     *
     * @param messagingContext interceptor context
     */
    protected void update(MessagingInterceptorContext messagingContext) {
        messagingContext.channelName(channelName);
//        initParameters(ParamType.INDEXED);
//
//        if (paramType == ParamType.NAMED) {
//            messagingContext.channel(channel, parameters.namedParams());
//        } else {
//            messagingContext.channel(channel, parameters.indexedParams());
//        }
//        messagingContext.channelType(channelType());
    }


    protected String messagingType() {
        return messagingType;
    }

}
