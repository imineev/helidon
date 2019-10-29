package io.helidon.messagingclient.kafka;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletionStage;

public class KafkaSink implements SubscriberBuilder {
    CompletionSubscriber completionSubscriber;
    public KafkaSink(String servers) {

    }

    public SubscriberBuilder<? extends Message<?>, Void> getSink() {
        return null;
    }

    @Override
    public CompletionSubscriber build() {
        completionSubscriber =  completionSubscriber!=null?completionSubscriber:new KafkaCompletionSubscriber();
        return completionSubscriber;
    }

    @Override
    public CompletionSubscriber build(ReactiveStreamsEngine reactiveStreamsEngine) {
        return null;
    }





    private class KafkaCompletionSubscriber implements CompletionSubscriber {
        @Override
        public CompletionStage getCompletion() {
            return null;
        }

        @Override
        public void onSubscribe(Subscription subscription) {

        }

        @Override
        public void onNext(Object o) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {

        }
    }
}
