package test;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

class Test {
    public static void main(String[] args) throws Exception {
//        Connection nc = Nats.connect("nats://myhost:4222");
        Connection nc = Nats.connect("nats://0.0.0.0:4222");
//        Connection nc = Nats.connect();
        System.out.println("Test.main nc:"+nc);

        nc.publish("subject", "hello world".getBytes(StandardCharsets.UTF_8));

        Subscription sub = nc.subscribe("subject");
        Message msg = sub.nextMessage(Duration.ofMillis(500));

        nc.publish("subject", "hello world".getBytes(StandardCharsets.UTF_8));
        System.out.println("sleep 20 seconds...");
        Thread.sleep(1000 * 20);
        String response = new String(msg.getData(), StandardCharsets.UTF_8);
        System.out.println("Test.main response:" + response);
    }
}