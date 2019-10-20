package io.helidon.messagingclient.jms;

public class JMSMessagingClientConfig {

    private final String url;
    private final String username;
    private final String password;
    private final String topic;
    private final String queue;
    private final String messagetype;
    private final int numberofmessagestoconsume;

    JMSMessagingClientConfig(String url, String username, String password, String topic, String queue,
                               String messagetype, int numberofmessagestoconsume) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.queue = queue;
        this.messagetype = messagetype;
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

    String messagetype() { return messagetype;  }

    int numberofmessagestoconsume() { return numberofmessagestoconsume;  }

}

