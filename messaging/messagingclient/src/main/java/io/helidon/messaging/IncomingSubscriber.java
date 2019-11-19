package io.helidon.messaging;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


public class IncomingSubscriber implements Subscriber<Message<?>> {
    private IncomingMessagingService incomingMessagingService;
    private ProcessingMessagingService processingMessagingService;
    private List<IncomingConnectorFactory> incomingConnectorFactories = new ArrayList<>();
    private boolean isAQ;
    private Outgoing outgoing;

    public IncomingSubscriber(IncomingMessagingService incomingMessagingService, String channelName) {
        this.incomingMessagingService = incomingMessagingService;
    }

    public IncomingSubscriber(ProcessingMessagingService processingMessagingService, String channelName) {
        this.processingMessagingService = processingMessagingService;
    }

    public void addIncomingConnectionFactory(IncomingConnectorFactory incomingConnectorFactory) {
        incomingConnectorFactories.add(incomingConnectorFactory);
    }


    public void subscribe(Config config, Outgoing outgoing) {
        this.outgoing = outgoing;
        incomingConnectorFactories
                .get(0) //todo only supports one currently
                .getPublisherBuilder(config)
                .buildRs()
                .subscribe(this);
    }

    @Override
    public void onNext(Message<?> message) {
        System.out.println("IncomingSubscriber.onNext message:" + message);
        try {
            if (processingMessagingService != null) {
                MessageWithConnectionAndSession messageWithConnectionAndSession =
                        message.unwrap(MessageWithConnectionAndSession.class);
                Channels.getInstance().addProcessingMessagingService(
                        messageWithConnectionAndSession.getChannelName(), processingMessagingService);
                Channels.getInstance().addProcessingMessagingServiceHolder(
                        messageWithConnectionAndSession.getChannelName(), message,
                        messageWithConnectionAndSession.getSession(),
                        messageWithConnectionAndSession.getConnection());
                outgoing.outgoing();
            } else { // is just incoming
                if (isAQ) {
                    MessageWithConnectionAndSession messageWithConnectionAndSession =
                            message.unwrap(MessageWithConnectionAndSession.class);
                    incomingMessagingService.onIncoming(messageWithConnectionAndSession,
                            messageWithConnectionAndSession.getConnection(),
                            messageWithConnectionAndSession.getSession());
                } else {
                    incomingMessagingService.onIncoming(message, null, null);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("IncomingSubscriber.onError:" + t);
        //todo
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        System.out.println("IncomingSubscriber.onSubscribe subscription:" + subscription);
        subscription.request(1);
    }

    @Override
    public void onComplete() {
        System.out.println("IncomingSubscriber.onComplete");
    }

    void setAQ(boolean b) {
        isAQ = b;
    }

}

