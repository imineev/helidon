package io.helidon.messagingclient.jms;

import io.helidon.config.Config;
import io.helidon.messagingclient.MessagingClientException;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ConnectionPool {

    static ConnectionPool create(Config config) {
        return ConnectionPool.builder()
                .config(config)
                .build();
    }

    /**
     * Create a fluent API builder for a JDBC Connection pool based on URL, username and password.
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
     * The type of this database - if better details than {@value JMSMessagingClientProvider#MESSAGING_TYPE} is
     * available, return it. This could be "jmessagingc:aq" etc.
     *
     * @return type of this database
     */
    default String messagingType() {
        return JMSMessagingClientProvider.MESSAGING_TYPE;
    }


    final class Builder implements io.helidon.common.Builder<ConnectionPool> {
        //jmessagingc:mysql://127.0.0.1:3306/pokemon?useSSL=false
        private static final Pattern URL_PATTERN = Pattern.compile("(\\w+:\\w+):.*");
        private String url;
        private String username;
        private String password;

        private Builder() {
        }

        @Override
        public ConnectionPool build() {
//        public ConnectionPool build() {
            final String url = this.url;
            final String username = this.username;
            final String password = this.password;

            Matcher matcher = URL_PATTERN.matcher(url);
            String messagingType;
            if (matcher.matches()) {
                messagingType = matcher.group(1);
            } else {
                messagingType = JMSMessagingClientProvider.MESSAGING_TYPE;
            }
            return new ConnectionPool() {

                QueueConnectionFactory getQueueConnectionFactory() {
/**
                    Constructor constructor = OracleDataSource.class.getConstructor(String.class);
//                    DataSource datasource = (DataSource)constructor.newInstance(url, user, pw;
                    OracleDataSource datasource = (OracleDataSource)constructor.newInstance();
                    datasource.setURL...
                    return AQjmsFactory.getQueueConnectionFactory(dataSource); //todo paul
 */
return null;
                }

                @Override
                public QueueConnection connection() {
                    try {
                        return getQueueConnectionFactory().createQueueConnection();
                    } catch (JMSException e) {
                        throw new MessagingClientException("Failed to create a connection to " + url, e);
                    }
                }

                @Override
                public String messagingType() {
                    return messagingType;
                }
            };
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
