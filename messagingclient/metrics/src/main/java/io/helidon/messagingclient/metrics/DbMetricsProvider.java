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
package io.helidon.messagingclient.metrics;

import io.helidon.config.Config;
import io.helidon.messagingclient.MessagingClientException;
import io.helidon.messagingclient.MessagingInterceptor;
import io.helidon.messagingclient.spi.MessagingInterceptorProvider;

/**
 * Service for Messagingmetrics.
 */
public class DbMetricsProvider implements MessagingInterceptorProvider {
    @Override
    public String configKey() {
        return "metrics";
    }

    @Override
    public MessagingInterceptor create(Config config) {
        String type = config.get("type").asString().orElse("COUNTER");
        switch (type) {
        case "COUNTER":
            return MessagingCounter.create(config);
        case "METER":
            return MessagingMeter.create(config);
        case "TIMER":
            return MessagingTimer.create(config);
        default:
            throw new MessagingClientException("Metrics type " + type + " is not supported through service loader");
        }
    }
}
