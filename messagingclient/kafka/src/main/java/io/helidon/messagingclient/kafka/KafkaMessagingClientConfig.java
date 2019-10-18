package io.helidon.messagingclient.kafka;

public class KafkaMessagingClientConfig {

    private final String url;
    private final String username;
    private final String password;
    private final String credDb;

    KafkaMessagingClientConfig(String url, String username, String password, String credDb) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.credDb = credDb;
    }

    String url() {
        return url;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    String credDb() {
        return credDb;
    }

}

