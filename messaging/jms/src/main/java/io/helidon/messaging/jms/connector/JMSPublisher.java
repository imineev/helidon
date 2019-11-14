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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.function.Consumer;

public class JMSPublisher<K, V> implements Publisher<MessageWithConnectionAndSession<K, V>> {

    private Consumer<Subscriber<? super MessageWithConnectionAndSession<K, V>>> publisher;

    public JMSPublisher(Consumer<Subscriber<? super MessageWithConnectionAndSession<K, V>>> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super MessageWithConnectionAndSession<K, V>> s) {
        System.out.println("JMSPublisher.subscribe");
        publisher.accept(s);
    }

}
