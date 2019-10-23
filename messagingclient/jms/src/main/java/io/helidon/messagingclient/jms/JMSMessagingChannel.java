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


    public CompletionStage<HelidonMessage> incoming(MessageProcessor testMessageProcessor) {
        LOGGER.fine(() -> String.format("JMSMessagingChannel.channel incoming"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doIncoming(messagingContextFuture, channelFuture, queryFuture);
    }


    protected CompletionStage<HelidonMessage> doIncoming(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                         CompletableFuture<Void> channelFuture,
                                                         CompletableFuture<HelidonMessage> queryFuture) {
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
                        new JMSMessage(receiveMessages(config.queue(), config.messagetype())));
            });
            return queryFuture;
        });
    }

    // todo this/future processing is incomplete...
    public Object receiveMessages(String queueName, String messageType) {
        QueueConnection connection = null;
        javax.jms.Queue queue;
        QueueSession session =null;
        javax.jms.Message msg;
        try {
            connection = connectionPool.connection();
            session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createReceiver(queue);
            connection.start();
            System.out.println("channel before receive queue:" + queue);
            msg = consumer.receive();
            System.out.println("channel message:" + msg);
            String action = "";
            String messageTxt = "";
            if (messageType.equals("text")) {
                TextMessage message = (TextMessage)msg;
                messageTxt = message.getText();
                System.out.println("channel message (null may be expected):" + messageTxt);
                action = message.getStringProperty("action");
                System.out.println("channel message action:" + action);
                int orderid = message.getIntProperty("orderid");
                System.out.println("channel message orderid:" + orderid);
            }
            else if (messageType.equals("map")){
                MapMessage mapmsg = (MapMessage) msg;
                System.out.println("----->" + mapmsg.getStringProperty("orderid"));
                System.out.println("----->" + mapmsg.getString("orderid"));
                System.out.println("----->" + mapmsg.getStringProperty("service"));
                System.out.println("----->" + mapmsg.getString("service"));
                System.out.println("----->" + mapmsg.getStringProperty("action"));
                System.out.println("----->" + mapmsg.getString("action"));
                System.out.println("----->" + mapmsg.getString("x-request-id"));
            } else throw new Exception("channel unknown message type:" + messageType);
            session.commit();
            session.close();
            connection.close();
            return action;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (session != null) session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        } finally {
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
        return null;
    }



    @Override
    public CompletionStage<HelidonMessage> outgoing(MessageProcessor testMessageProcessor, HelidonMessage message) {
        LOGGER.fine(() -> String.format("JMSMessagingChannel.channel incoming"));
        CompletableFuture<HelidonMessage> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .channelFuture(channelFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doOutgoing(messagingContextFuture, channelFuture, queryFuture);
    }

    protected CompletionStage<HelidonMessage> doOutgoing(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                         CompletableFuture<Void> channelFuture,
                                                         CompletableFuture<HelidonMessage> queryFuture) {
        System.out.println("JMSMessagingChannel.doOutgoing");
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
                        new JMSMessage(sendMessage(config.queue(), config.messagetype())));
            });
            return queryFuture;
        });
    }

    public String sendMessage(String queueName, String messageType)  {
        QueueConnection connection = null;
        javax.jms.Queue queue;
        QueueSession session =null;
        javax.jms.Message msg;
        try {
            connection = connectionPool.connection();
            session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
            queue = session.createQueue(queueName);
            String messageTxt = "test message";
            session.createSender(queue).send(null); //TextMessage
            session.commit();
            System.out.println("sendMessage committed messageTxt:" + messageTxt + " on queue:" + queue);
            session.close();
            connection.close();
            return queue.toString();
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
