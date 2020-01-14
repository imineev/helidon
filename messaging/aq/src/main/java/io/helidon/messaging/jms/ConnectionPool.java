package io.helidon.messaging.jms;

import io.helidon.config.Config;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import oracle.jms.AQjmsFactory;
import oracle.jdbc.pool.OracleDataSource;

public interface ConnectionPool {

    static ConnectionPool create(Config config) {
        return ConnectionPool.builder()
                .config(config)
                .build();
    }

    /**
     * Create a fluent API builder for a JDBC Connection pool based on URL, username and password.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Return a connection from the pool.
     * The call to {@link javax.jms.QueueConnection#close()} will return that connection to the pool.
     *
     * @return a connection read to execute statements
     */
    QueueConnection connection();

    /**
     *
     * @return type of this messaging
     */
    default String messagingType() {
        return "JMS";
    }


    final class Builder implements io.helidon.common.Builder<ConnectionPool> {
        private String url;
        private String username;
        private String password;

        private Builder() {
        }

        @Override
        public ConnectionPool build() {
            return getConnectionPool();
        }

        private ConnectionPool getConnectionPool()  {
            try {
                OracleDataSource aqDataSource = new oracle.jdbc.pool.OracleDataSource();
                aqDataSource.setURL(url);
                aqDataSource.setUser(username);
                aqDataSource.setPassword(password);
                System.out.println("JMSMessagingClientProviderBuilder.setConnectionPool " +
                        " for url:" + url + " username:" + username + " password:" + password +
                        " aqDataSource:" + aqDataSource);
                QueueConnectionFactory qcf = AQjmsFactory.getQueueConnectionFactory(aqDataSource);
                System.out.println("JMSMessagingClientProviderBuilder.setConnectionPool " +
                        "qcf:" + qcf);
                ConnectionPool connectionPool = new ConnectionPool() {
                    @Override
                    public QueueConnection connection() {
                        try {
                            return qcf.createQueueConnection();
                        } catch (JMSException e) {
                            e.printStackTrace();
                            return null; //todo throw messaging exception
                        }
                    }

                    @Override
                    public String messagingType() {
                        return "AQJMS-queue";
                    }
                };
                System.out.println("JMSMessagingClientProviderBuilder.setConnectionPool " +
                        "connectionPool.connection():" + connectionPool.connection());
                return connectionPool;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public Builder config(Config config) {
            config.get("url").asString().ifPresent(this::url);
            config.get("username").asString().ifPresent(this::username);
            config.get("password").asString().ifPresent(this::password);
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }
    }

}
