package io.helidon.messagingclient;

/**
 * Implements methods that do not require implementation for each provider.
 */
public abstract class AbstractMessagingExecute {

    private final MessagingOperations operations;

    public AbstractMessagingExecute(MessagingOperations operations) {
        this.operations = operations;
    }
}
