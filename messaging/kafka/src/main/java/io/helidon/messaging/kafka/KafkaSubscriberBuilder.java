package io.helidon.messaging.kafka;

import io.helidon.messaging.kafka.connector.KafkaMessage;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

public class KafkaSubscriberBuilder<K, V> implements SubscriberBuilder {

    public KafkaSubscriberBuilder() {

    }

    //public class KafkaSubscriberBuilder implements SubscriberBuilder<? extends Message<?>, Void> {
    @Override
    public CompletionSubscriber<? extends Message<?>, Void> build() {
        return null;
    }

    @Override
    public CompletionSubscriber<? extends Message<?>, Void> build(ReactiveStreamsEngine reactiveStreamsEngine) {
        return null;
    }
}
