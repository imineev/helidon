/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.messaging.jms.connector;

import io.helidon.messaging.MessageWithConnectionAndSession;
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
import java.util.concurrent.CompletionStage;
import java.util.function.*;
import java.util.stream.Collector;

public class JMSPublisherBuilder<K, V> implements PublisherBuilder<MessageWithConnectionAndSession<K, V>> {

    private Consumer<Subscriber<? super MessageWithConnectionAndSession<K, V>>> publisher;

    public JMSPublisherBuilder(Consumer<Subscriber<? super MessageWithConnectionAndSession<K, V>>> publisher) {
        this.publisher = publisher;
    }

    @Override
    public Publisher<MessageWithConnectionAndSession<K, V>> buildRs() {
        return new JMSPublisher<K, V>(publisher);
    }


    @Override
    public <R> PublisherBuilder<R> map(Function<? super MessageWithConnectionAndSession<K, V>, ? extends R> function) {
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMap(Function<? super MessageWithConnectionAndSession<K, V>, ? extends PublisherBuilder<? extends S>> function) {
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMapRsPublisher(Function<? super MessageWithConnectionAndSession<K, V>, ? extends Publisher<? extends S>> function) {
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMapCompletionStage(Function<? super MessageWithConnectionAndSession<K, V>, ? extends CompletionStage<? extends S>> function) {
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMapIterable(Function<? super MessageWithConnectionAndSession<K, V>, ? extends Iterable<? extends S>> function) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> filter(Predicate<? super MessageWithConnectionAndSession<K, V>> predicate) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> distinct() {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> limit(long l) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> skip(long l) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> takeWhile(Predicate<? super MessageWithConnectionAndSession<K, V>> predicate) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> dropWhile(Predicate<? super MessageWithConnectionAndSession<K, V>> predicate) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> peek(Consumer<? super MessageWithConnectionAndSession<K, V>> consumer) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> onError(Consumer<Throwable> consumer) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> onTerminate(Runnable runnable) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> onComplete(Runnable runnable) {
        return null;
    }

    @Override
    public CompletionRunner<Void> forEach(Consumer<? super MessageWithConnectionAndSession<K, V>> consumer) {
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
    public CompletionRunner<MessageWithConnectionAndSession<K, V>> reduce(MessageWithConnectionAndSession<K, V> kvMessageWithConnectionAndSession, BinaryOperator<MessageWithConnectionAndSession<K, V>> binaryOperator) {
        return null;
    }

    @Override
    public CompletionRunner<Optional<MessageWithConnectionAndSession<K, V>>> reduce(BinaryOperator<MessageWithConnectionAndSession<K, V>> binaryOperator) {
        return null;
    }

    @Override
    public CompletionRunner<Optional<MessageWithConnectionAndSession<K, V>>> findFirst() {
        return null;
    }

    @Override
    public <R, A> CompletionRunner<R> collect(Collector<? super MessageWithConnectionAndSession<K, V>, A, R> collector) {
        return null;
    }

    @Override
    public <R> CompletionRunner<R> collect(Supplier<R> supplier, BiConsumer<R, ? super MessageWithConnectionAndSession<K, V>> biConsumer) {
        return null;
    }

    @Override
    public CompletionRunner<List<MessageWithConnectionAndSession<K, V>>> toList() {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> onErrorResume(Function<Throwable, ? extends MessageWithConnectionAndSession<K, V>> function) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> onErrorResumeWith(Function<Throwable, ? extends PublisherBuilder<? extends MessageWithConnectionAndSession<K, V>>> function) {
        return null;
    }

    @Override
    public PublisherBuilder<MessageWithConnectionAndSession<K, V>> onErrorResumeWithRsPublisher(Function<Throwable, ? extends Publisher<? extends MessageWithConnectionAndSession<K, V>>> function) {
        return null;
    }

    @Override
    public CompletionRunner<Void> to(Subscriber<? super MessageWithConnectionAndSession<K, V>> subscriber) {
        return null;
    }

    @Override
    public <R> CompletionRunner<R> to(SubscriberBuilder<? super MessageWithConnectionAndSession<K, V>, ? extends R> subscriberBuilder) {
        return null;
    }

    @Override
    public <R> PublisherBuilder<R> via(ProcessorBuilder<? super MessageWithConnectionAndSession<K, V>, ? extends R> processorBuilder) {
        return null;
    }

    @Override
    public <R> PublisherBuilder<R> via(Processor<? super MessageWithConnectionAndSession<K, V>, ? extends R> processor) {
        return null;
    }



    @Override
    public Publisher<MessageWithConnectionAndSession<K, V>> buildRs(ReactiveStreamsEngine reactiveStreamsEngine) {
        return null;
    }
}