package io.helidon.messagingclient.kafka;


import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collector;

public class KafkaSource<K, V> implements  PublisherBuilder {  // <? extends Message<?>>
    private final PublisherBuilder<? extends Message<?>> source;
    private final KafkaConsumer<K, V> consumer;

    KafkaSource(String servers) {
        consumer = null;
        this.source = null;
    }

    PublisherBuilder<? extends Message<?>> getSource() {
        return source;
    }

    @Override
    public PublisherBuilder map(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder flatMap(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder flatMapRsPublisher(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder flatMapCompletionStage(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder flatMapIterable(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder filter(Predicate predicate) {
        return null;
    }

    @Override
    public PublisherBuilder distinct() {
        return null;
    }

    @Override
    public PublisherBuilder limit(long l) {
        return null;
    }

    @Override
    public PublisherBuilder skip(long l) {
        return null;
    }

    @Override
    public PublisherBuilder takeWhile(Predicate predicate) {
        return null;
    }

    @Override
    public PublisherBuilder dropWhile(Predicate predicate) {
        return null;
    }

    @Override
    public PublisherBuilder peek(Consumer consumer) {
        return null;
    }

    @Override
    public PublisherBuilder onTerminate(Runnable runnable) {
        return null;
    }

    @Override
    public PublisherBuilder onComplete(Runnable runnable) {
        return null;
    }

    @Override
    public CompletionRunner<Void> forEach(Consumer consumer) {
        return null;
    }

    @Override
    public CompletionRunner<Void> ignore() {
        return null;
    }

    @Override
    public CompletionRunner<Void> cancel() {
        return null;
    }

    @Override
    public CompletionRunner reduce(Object o, BinaryOperator binaryOperator) {
        return null;
    }

    @Override
    public CompletionRunner<Optional> reduce(BinaryOperator binaryOperator) {
        return null;
    }

    @Override
    public CompletionRunner<Optional> findFirst() {
        return null;
    }

    @Override
    public CompletionRunner<List> toList() {
        return null;
    }

    @Override
    public CompletionRunner<Void> to(Subscriber subscriber) {
        return null;
    }

    @Override
    public Publisher buildRs() {
        return new Publisher() {
            @Override
            public void subscribe(Subscriber subscriber) {

            }
        };
    }

    @Override
    public Publisher buildRs(ReactiveStreamsEngine reactiveStreamsEngine) {
        return null;
    }

    @Override
    public PublisherBuilder via(Processor processor) {
        return null;
    }

    @Override
    public PublisherBuilder via(ProcessorBuilder processorBuilder) {
        return null;
    }

    @Override
    public CompletionRunner to(SubscriberBuilder subscriberBuilder) {
        return null;
    }

    @Override
    public PublisherBuilder onErrorResumeWithRsPublisher(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder onErrorResumeWith(Function function) {
        return null;
    }

    @Override
    public PublisherBuilder onErrorResume(Function function) {
        return null;
    }

    @Override
    public CompletionRunner collect(Supplier supplier, BiConsumer biConsumer) {
        return null;
    }

    @Override
    public CompletionRunner collect(Collector collector) {
        return null;
    }

    @Override
    public PublisherBuilder onError(Consumer consumer) {
        return null;
    }
}
