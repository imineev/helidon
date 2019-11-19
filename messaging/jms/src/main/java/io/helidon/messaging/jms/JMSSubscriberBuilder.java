package io.helidon.messaging.jms;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletionStage;

public class JMSSubscriberBuilder<K, V> implements SubscriberBuilder {
    @Override
    public CompletionSubscriber build() {
        return new CompletionSubscriber() {
            @Override
            public CompletionStage getCompletion() {
                System.out.println("JMSSubscriberBuilder.getCompletion");
                return null;
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                System.out.println("JMSSubscriberBuilder.onSubscribe");
                subscription.request(1);
            }

            @Override
            public void onNext(Object o) {
                System.out.println("JMSSubscriberBuilder.onNext");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("JMSSubscriberBuilder.onError");
            }

            @Override
            public void onComplete() {
                System.out.println("JMSSubscriberBuilder.onComplete");
            }
        };
    }

    @Override
    public CompletionSubscriber build(ReactiveStreamsEngine reactiveStreamsEngine) {
        return null;
    }
}
