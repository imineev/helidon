package io.helidon.messagingclient.kafka;

class KafkaMessagingClientConfig {

    private final String url;
    private final String username;
    private final String password;
    private final String topic;
    private final String queue;
    private final String bootstrapservers;
    private final int numberofmessagestoconsume;

    //todo basically everything from consumer.properties and producer.properties and file location for same
    KafkaMessagingClientConfig(String url, String username, String password, String topic, String queue,
                               String bootstrapservers, int numberofmessagestoconsume) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.queue = queue;
        this.bootstrapservers = bootstrapservers;
        this.numberofmessagestoconsume = numberofmessagestoconsume;
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

    String topic() { return topic;  }

    String queue() { return queue;  }

    String bootstrapservers() { return bootstrapservers;  }

    int numberofmessagestoconsume() { return numberofmessagestoconsume;  }

}

