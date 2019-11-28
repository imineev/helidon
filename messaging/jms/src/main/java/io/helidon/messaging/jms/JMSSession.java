package io.helidon.messaging.jms;

import io.helidon.messaging.Session;

public class JMSSession implements Session {
    javax.jms.Session session;

    public JMSSession(javax.jms.Session session) {
        this.session = session;
    }

    @Override
    public Object getSession() {
        return session;
    }

    @Override
    public Object unwrap(Class unwrapType) { //todo
        return session;
    }
}
