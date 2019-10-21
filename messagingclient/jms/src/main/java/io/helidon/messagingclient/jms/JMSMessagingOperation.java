package io.helidon.messagingclient.jms;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.messagingclient.*;
import io.helidon.messagingclient.Message;
import io.helidon.messagingclient.common.InterceptorSupport;
import javax.jms.*;
import javax.jms.Session;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class JMSMessagingOperation implements MessagingOperation {
    private final ExecutorService executorService;
    private final ConnectionPool connectionPool;
    private final JMSMessagingClientConfig config;
    private final InterceptorSupport interceptors;
    private final String messagingType = "JMS";
    private final String operationName = "all"; //todo
    private final MessagingOperationType operationType = MessagingOperationType.MESSAGING;

    private static final Logger LOGGER = Logger.getLogger(JMSMessagingOperation.class.getName());
//
    public JMSMessagingOperation(ExecutorService executorService, InterceptorSupport interceptors,
                                 ConnectionPool connectionPool, JMSMessagingClientConfig config) {
        this.executorService = executorService;
        this.interceptors = interceptors;
        this.config = config;
        this.connectionPool = connectionPool;
    }


    public CompletionStage<Message> incoming(MessageProcessor testMessageProcessor) {
        LOGGER.fine(() -> String.format("JMSMessagingOperation.operation incoming"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doIncoming(messagingContextFuture, operationFuture, queryFuture);
    }

    protected CompletionStage<Message> doIncoming(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                  CompletableFuture<Void> operationFuture,
                                                  CompletableFuture<Message> queryFuture) {
        System.out.println("JMSMessagingOperation.doIncoming");
        // query and operation future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        return messagingContextFuture.thenCompose(messagingContext -> {
            executorService.submit(() -> {
                operationFuture.complete(null);
                queryFuture.complete(
                        new JMSMessage(receiveMessages(config.queue(), config.messagetype())));
            });
            return queryFuture;
        });
    }

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
            System.out.println("operation before receive queue:" + queue);
            msg = consumer.receive();
            System.out.println("operation message:" + msg);
            String action = "";
            String messageTxt = "";
            if (messageType.equals("text")) {
                TextMessage message = (TextMessage)msg;
                messageTxt = message.getText();
                System.out.println("operation message (null may be expected):" + messageTxt);
                action = message.getStringProperty("action");
                System.out.println("operation message action:" + action);
                int orderid = message.getIntProperty("orderid");
                System.out.println("operation message orderid:" + orderid);
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
            } else throw new Exception("operation unknown message type:" + messageType);
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
    public CompletionStage<Message> outgoing(MessageProcessor testMessageProcessor) {
        LOGGER.fine(() -> String.format("JMSMessagingOperation.operation incoming"));
        CompletableFuture<Message> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        MessagingInterceptorContext messagingContext = MessagingInterceptorContext.create(messagingType())
                .resultFuture(queryFuture)
                .operationFuture(operationFuture);
        update(messagingContext);
        CompletionStage<MessagingInterceptorContext> messagingContextFuture = invokeInterceptors(messagingContext);
        return doOutgoing(messagingContextFuture, operationFuture, queryFuture);
    }

    protected CompletionStage<Message> doOutgoing(CompletionStage<MessagingInterceptorContext> messagingContextFuture,
                                                  CompletableFuture<Void> operationFuture,
                                                  CompletableFuture<Message> queryFuture) {
        System.out.println("JMSMessagingOperation.doOutgoing");
        // query and operation future must always complete either OK, or exceptionally
        messagingContextFuture.exceptionally(throwable -> {
            operationFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });
        return messagingContextFuture.thenCompose(messagingContext -> {
            executorService.submit(() -> {
                operationFuture.complete(null);
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

        for (MessagingInterceptor interceptor : interceptors.interceptors(operationType(), operationName())) {
            result = result.thenCompose(interceptor::operation);
        }

        return result;
    }

    private MessagingOperationType operationType() {
        return operationType;
    }

    private String operationName() {
        return operationName;
    }


    /**
     * Update the interceptor context with the operation name, operation and
     * operation parameters.
     *
     * @param messagingContext interceptor context
     */
    protected void update(MessagingInterceptorContext messagingContext) {
        messagingContext.operationName(operationName);
//        initParameters(ParamType.INDEXED);
//
//        if (paramType == ParamType.NAMED) {
//            messagingContext.operation(operation, parameters.namedParams());
//        } else {
//            messagingContext.operation(operation, parameters.indexedParams());
//        }
//        messagingContext.operationType(operationType());
    }


    protected String messagingType() {
        return messagingType;
    }

}
