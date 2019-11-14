package io.helidon.messaging;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


public class IncomingSubscriber implements Subscriber<Message<?>> {
    private final IncomingMessagingService incomingMessagingService;
    private List<IncomingConnectorFactory> incomingConnectorFactories = new ArrayList<>();
    private boolean isAQ;

    public IncomingSubscriber(IncomingMessagingService incomingMessagingService, String channelName) {
        System.out.println("IncomingSubscriber.IncomingSubscriber" +
                " incomingMessagingService = [" + incomingMessagingService + "], channelName = [" + channelName + "]");
        this.incomingMessagingService = incomingMessagingService;
    }

    public void addIncomingConnectionFactory(IncomingConnectorFactory incomingConnectorFactory) {
        System.out.println("IncomingSubscriber.addIncomingConnectionFactory " +
                "incomingConnectorFactory:" + incomingConnectorFactory);
        incomingConnectorFactories.add(incomingConnectorFactory);
    }


    public void subscribe(Config config) {
        System.out.println("IncomingSubscriber.subscribe incomingConnectorFactory:" +
                incomingConnectorFactories.get(0));
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
            if (isAQ) {
                MessageWithConnectionAndSession messageWithConnectionAndSession =
                        message.unwrap(MessageWithConnectionAndSession.class);
                incomingMessagingService.onIncoming(messageWithConnectionAndSession,
                        messageWithConnectionAndSession.getConnection(),
                        messageWithConnectionAndSession.getSession());
            } else {
                incomingMessagingService.onIncoming(message, null, null);
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

    public void setAQ(boolean b) {
        isAQ = b;
    }
}

