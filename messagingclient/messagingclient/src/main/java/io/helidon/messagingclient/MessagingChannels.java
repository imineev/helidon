package io.helidon.messagingclient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.helidon.config.Config;

/**
 * Configuration of channels to be used by messaging provider.
 */
@FunctionalInterface
public interface MessagingChannels {
    /**
     * Get channel text for a named channel.
     *
     * @param name name of the channel
     * @return text of the channel (such as SQL code for SQL-based messaging channels)
     * @throws MessagingClientException in case the channel name does not exist
     */
    String channel(String name) throws MessagingClientException;

    /**
     * Builder of channels.
     *
     * @return a builder to customize channels
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create channels from configuration.
     * Channel configuration is expected to be a map of name to channel pairs.
     *
     * @param config configuration of the channels
     * @return channels as read from the configuration
     */
    static MessagingChannels create(Config config) {
        return MessagingChannels.builder()
                .config(config)
                .build();
    }

    /**
     * Fluent API builder for {@link MessagingChannels}.
     */
    class Builder implements io.helidon.common.Builder<MessagingChannels> {
        private final Map<String, String> configuredChannels = new HashMap<>();

        /**
         * Add named messaging channel to messaging configuration..
         *
         * @param name      messaging channel name
         * @param channel messaging channel {@link String}
         * @return messaging provider builder
         */
        public Builder addChannel(String name, String channel) {
            Objects.requireNonNull(name, "Channel name must be provided");
            Objects.requireNonNull(channel, "Channel body must be provided");
            configuredChannels.put(name, channel);
            return this;
        }

        /**
         * Set channels from configuration. Each key in the current node is treated as a name of the channel,
         * each value as the channel content.
         *
         * @param config config node located on correct node
         * @return updated builder instance
         */
        public Builder config(Config config) {
            config.detach().asMap()
                    .ifPresent(configuredChannels::putAll);
            return this;
        }

        @Override
        public MessagingChannels build() {
            return new MessagingChannels() {
                private final Map<String, String> channels = new HashMap<>(configuredChannels);

                @Override
                public String channel(String name) {
                    String channel = channels.get(name);

                    if (null == channel) {
                        throw new MessagingClientException("Channel named '" + name + "' is not defined");
                    }

                    return channel;
                }

            };
        }
    }
}

