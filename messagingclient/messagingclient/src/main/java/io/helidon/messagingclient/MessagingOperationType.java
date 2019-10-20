package io.helidon.messagingclient;

public enum MessagingOperationType {
    /**
     * Query is operation that returns zero or more results.
     */
    MESSAGING("m"),
    /**
     * The operation type is not yet knows (e.g. when invoking
     * {@link MessagingOperationOptions#filterForEndpoint(String)}
     */
    UNKNOWN("x");

    private final String prefix;

    MessagingOperationType(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Short prefix of this type.
     * This is used when generating a name for an unnamed operation.
     *
     * @return short prefix defining this type (should be very short)
     */
    public String prefix() {
        return prefix;
    }
}
