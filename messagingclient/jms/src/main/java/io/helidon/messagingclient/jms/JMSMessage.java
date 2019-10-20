package io.helidon.messagingclient.jms;

import io.helidon.messagingclient.Message;

public class JMSMessage implements Message {
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
