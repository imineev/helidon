package io.helidon.messaging;

import org.eclipse.microprofile.reactive.messaging.Message;

public interface ProcessingMessagingService {
    Message onProcessing(Message message, Connection connection, Session session) throws Exception;
}
