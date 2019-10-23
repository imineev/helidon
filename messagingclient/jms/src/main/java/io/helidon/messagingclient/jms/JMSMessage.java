package io.helidon.messagingclient.jms;

import io.helidon.messagingclient.HelidonMessage;

public class JMSMessage implements HelidonMessage {
    private Object string;

    public JMSMessage(Object string) {
        System.out.println("JMSMessage string:" + string);
        this.string = string;
    }

    @Override
    public String getString() {
        return string.toString();
    }

}
