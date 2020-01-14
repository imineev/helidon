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



import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.Destination;
import javax.jms.JMSException;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * JMS specific MP messaging message
 *
 * @param <K> kafka record key type
 * @param <V> kafka record value type
 */
public class JMSMessage<K, V> implements Message {

    private Message message;

    public JMSMessage(Message message) {
        this.message = message;
    }

    public Message getPayload() {
        return message;
    }

    public CompletionStage<Void> ack() {
        //TODO: implement acknowledge
        return new CompletableFuture<>();
    }

}
