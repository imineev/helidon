package io.helidon.messagingclient.jms;

import io.helidon.config.Config;
import io.helidon.messagingclient.MessagingClientException;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.AQ.AQQueueTable;
import oracle.AQ.AQQueueTableProperty;
import oracle.jms.AQjmsDestinationProperty;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;
import oracle.jdbc.pool.OracleDataSource;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.sql.DataSource;

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
     * The type of this messaging - if better details than {@value JMSMessagingClientProvider#MESSAGING_TYPE} is
     * available, return it. This could be "jmessagingc:aq" etc.
     *
     * @return type of this messaging
     */
    default String messagingType() {
        return JMSMessagingClientProvider.MESSAGING_TYPE;
    }


    final class Builder implements io.helidon.common.Builder<ConnectionPool> {
        private static final Pattern URL_PATTERN = Pattern.compile("(\\w+:\\w+):.*");
        private String url;
        private String username;
        private String password;

        private Builder() {
        }

        @Override
        public ConnectionPool build() {

            Matcher matcher = URL_PATTERN.matcher(url);
            String messagingType;
            if (matcher.matches()) {
                messagingType = matcher.group(1);
            } else {
                messagingType = JMSMessagingClientProvider.MESSAGING_TYPE;
            }
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
