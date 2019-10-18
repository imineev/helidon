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

import java.util.concurrent.CompletionStage;

import io.helidon.config.Config;

import io.helidon.messagingclient.MessagingInterceptor;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

/**
 * Meter for Helidon DB. This class implements the {@link MessagingInterceptor} and
 * can be configured either through a {@link io.helidon.messagingclient.MessagingClient.Builder} or through configuration.
 */
public final class MessagingMeter extends MessagingMetric<Meter> {
    private MessagingMeter(Builder builder) {
        super(builder);
    }

    /**
     * Create a meter from configuration.
     *
     * @param config configuration to read
     * @return a new meter
     * @see io.helidon.messagingclient.metrics.DbMetricBuilder#config(io.helidon.config.Config)
     */
    public static MessagingMeter create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Create a new meter using default configuration.
     * <p>By default the name format is {@code db.meter.operation-name}, where {@code operation-name}
     * is provided at runtime.
     *
     * @return a new meter
     */
    public static MessagingMeter create() {
        return builder().build();
    }

    /**
     * Create a new fluent API builder to create a new meter metric.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void executeMetric(Meter metric, CompletionStage<Void> aFuture) {
        aFuture
                .thenAccept(nothing -> {
                    if (measureSuccess()) {
                        metric.mark();
                    }
                })
                .exceptionally(throwable -> {
                    if (measureErrors()) {
                        metric.mark();
                    }
                    return null;
                });
    }

    @Override
    protected MetricType metricType() {
        return MetricType.COUNTER;
    }

    @Override
    protected Meter metric(MetricRegistry registry, Metadata meta) {
        return registry.meter(meta);
    }

    @Override
    protected String defaultNamePrefix() {
        return "db.meter.";
    }

    /**
     * Fluent API builder for {@link MessagingMeter}.
     */
    public static class Builder extends DbMetricBuilder<Builder> implements io.helidon.common.Builder<MessagingMeter> {
        @Override
        public MessagingMeter build() {
            return new MessagingMeter(this);
        }
    }
}
