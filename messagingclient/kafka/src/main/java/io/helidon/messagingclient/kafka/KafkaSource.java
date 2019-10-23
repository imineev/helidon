package io.helidon.messagingclient.kafka;


import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

public class KafkaSource<K, V> {
    private final PublisherBuilder<? extends Message<?>> source;
    private final KafkaConsumer<K, V> consumer;

    KafkaSource(String servers) {
        consumer = null;
        this.source = null;
    }

    PublisherBuilder<? extends Message<?>> getSource() {
        return source;
    }
}
