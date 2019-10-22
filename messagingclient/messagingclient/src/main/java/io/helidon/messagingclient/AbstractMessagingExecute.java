package io.helidon.messagingclient;

/**
 * Implements methods that do not require implementation for each provider.
 */
public abstract class AbstractMessagingExecute {

    private final MessagingChannels channels;

    public AbstractMessagingExecute(MessagingChannels channels) {
        this.channels = channels;
    }
}
