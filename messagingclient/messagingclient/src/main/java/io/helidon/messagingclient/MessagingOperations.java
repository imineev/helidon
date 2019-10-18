package io.helidon.messagingclient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.helidon.config.Config;

/**
 * Configuration of operations to be used by database provider.
 */
@FunctionalInterface
public interface MessagingOperations {
    /**
     * Get operation text for a named operation.
     *
     * @param name name of the operation
     * @return text of the operation (such as SQL code for SQL-based database operations)
     * @throws MessagingClientException in case the operation name does not exist
     */
    String operation(String name) throws MessagingClientException;

    /**
     * Builder of operations.
     *
     * @return a builder to customize operations
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create operations from configuration.
     * Operation configuration is expected to be a map of name to operation pairs.
     *
     * @param config configuration of the operations
     * @return operations as read from the configuration
     */
    static MessagingOperations create(Config config) {
        return MessagingOperations.builder()
                .config(config)
                .build();
    }

    /**
     * Fluent API builder for {@link MessagingOperations}.
     */
    class Builder implements io.helidon.common.Builder<MessagingOperations> {
        private final Map<String, String> configuredOperations = new HashMap<>();

        /**
         * Add named database operation to database configuration..
         *
         * @param name      database operation name
         * @param operation database operation {@link String}
         * @return database provider builder
         */
        public Builder addOperation(String name, String operation) {
            Objects.requireNonNull(name, "Operation name must be provided");
            Objects.requireNonNull(operation, "Operation body must be provided");
            configuredOperations.put(name, operation);
            return this;
        }

        /**
         * Set operations from configuration. Each key in the current node is treated as a name of the operation,
         * each value as the operation content.
         *
         * @param config config node located on correct node
         * @return updated builder instance
         */
        public Builder config(Config config) {
            config.detach().asMap()
                    .ifPresent(configuredOperations::putAll);
            return this;
        }

        @Override
        public MessagingOperations build() {
            return new MessagingOperations() {
                private final Map<String, String> operations = new HashMap<>(configuredOperations);

                @Override
                public String operation(String name) {
                    String operation = operations.get(name);

                    if (null == operation) {
                        throw new MessagingClientException("Operation named '" + name + "' is not defined");
                    }

                    return operation;
                }

            };
        }
    }
}

