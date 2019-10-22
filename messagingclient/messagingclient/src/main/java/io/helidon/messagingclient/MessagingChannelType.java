package io.helidon.messagingclient;

public enum MessagingChannelType {
    /**
     * Query is channel that returns zero or more results.
     */
    MESSAGING("m"),
    /**
     * The channel type is not yet knows (e.g. when invoking
     * {@link MessagingChannelOptions#filterForEndpoint(String)}
     */
    UNKNOWN("x");

    private final String prefix;

    MessagingChannelType(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Short prefix of this type.
     * This is used when generating a name for an unnamed channel.
     *
     * @return short prefix defining this type (should be very short)
     */
    public String prefix() {
        return prefix;
    }
}
