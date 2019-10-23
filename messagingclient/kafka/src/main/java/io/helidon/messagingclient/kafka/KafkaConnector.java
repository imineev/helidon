package io.helidon.messagingclient.kafka;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

public class KafkaConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {
    String servers;

    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        KafkaSource<Object, Object> source = new KafkaSource<>(servers);
        return source.getSource();
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        KafkaSink sink = new KafkaSink(servers);
        return sink.getSink();
    }

}

