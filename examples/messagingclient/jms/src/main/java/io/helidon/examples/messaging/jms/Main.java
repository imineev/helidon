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

package io.helidon.examples.messaging.jms;

import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.media.jsonb.server.JsonBindingSupport;
import io.helidon.media.jsonp.server.JsonSupport;
import io.helidon.messagingclient.MessagingClient;
import io.helidon.messagingclient.MessagingOperationType;
import io.helidon.messagingclient.health.MessagingClientHealthCheck;
import io.helidon.messagingclient.metrics.MessagingCounter;
import io.helidon.messagingclient.metrics.MessagingTimer;
import io.helidon.messagingclient.tracing.MessagingClientTracing;
import io.helidon.metrics.MetricsSupport;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;

import java.io.IOException;
import java.util.logging.LogManager;


public final class Main {

    private Main() {
    }

    public static void main(final String[] args) throws IOException {
        startServer();
    }

    static WebServer startServer() throws IOException {
        LogManager.getLogManager().readConfiguration(
                Main.class.getResourceAsStream("/logging.properties"));
        Config config = Config.create();
        ServerConfiguration serverConfig =
                ServerConfiguration.builder(config.get("server"))
                        .tracer(TracerBuilder.create("messaging-poc").buildAndRegister())
                        .build();
        WebServer server = WebServer.create(serverConfig, createRouting(config));
        server.start().thenAccept(ws -> {
            System.out.println(
                    "WEB server is up! http://localhost:" + ws.port() + "/");
        });
        server.whenShutdown().thenRun(() -> System.out.println("WEB server is DOWN. Good bye!"));
        return server;
    }


    private static Routing createRouting(Config config) {
        Config messagingConfig = config.get("messaging-jms-demo");
        System.out.println("Main.createRouting messagingsourceConfig=" + config.get("source").name());

        MessagingClient messagingClient = MessagingClient.builder(messagingConfig)
                // add an interceptor to named/filters of operation(s)
                .addInterceptor(MessagingCounter.create(),  //todo would be from config
                        "subscribe-with-orderpattern", "send-with-order-props", "filter-orders")
                // add an interceptor to operation type(s)
                .addInterceptor(MessagingTimer.create(), MessagingOperationType.MESSAGING, MessagingOperationType.UNKNOWN)
                // add an interceptor to all operations
                .addInterceptor(MessagingClientTracing.create())
                .build();

        HealthSupport health = HealthSupport.builder()
                .add(MessagingClientHealthCheck.create(messagingClient))
                .build();
        // for headless can do here instead of pokemon service...
        return Routing.builder()
                .register("/messaging", JsonSupport.create())
                .register("/messaging", JsonBindingSupport.create())
                .register(health)                   // Health at "/health"
                .register(MetricsSupport.create())  // Metrics at "/metrics"
                .register("/messaging", new PokemonService(messagingClient))
                .build();
    }


}
