package io.helidon.messaging.jms;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

public class JMSSubscriberBuilder<K, V> implements SubscriberBuilder {
    @Override
    public CompletionSubscriber build() {
        return null;
    }

    @Override
    public CompletionSubscriber build(ReactiveStreamsEngine reactiveStreamsEngine) {
        return null;
    }
}
