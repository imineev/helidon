package io.helidon.messaging.jms;

import io.helidon.messaging.Connection;

public class JDBCConnection implements Connection {
    java.sql.Connection connection;
    public JDBCConnection(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public Object getConnection() {
        return connection;
    }

    @Override
    public Object unwrap(Class unwrapType) { //todo
        return connection;
    }
}
