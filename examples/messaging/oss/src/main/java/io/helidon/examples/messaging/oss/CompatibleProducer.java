package io.helidon.examples.messaging.oss;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;

public class CompatibleProducer {

    public void produce() {
//        String authToken = System.getenv("AUTH_TOKEN");
//        String tenancyName = System.getenv("TENANCY_NAME");
//        String username = System.getenv("STREAMING_USERNAME");
//        String streamPoolId = System.getenv("COMPARTMENT_ID");
//        String topicName = System.getenv("TOPIC_NAME");
//        String tenancyName = "ocid1.tenancy.oc1..aaaaaaaaogcg5o3giyldy7yybxphgnodvlgr4xuxv7fn5d7prjvt67qqplia";
//        String username = "ocid1.user.oc1..aaaaaaaajliqydpf6pblr2k6a4ivazjtftmk6af5seozhkel3khbzmsxngpq";
//        String streamPoolId = "ocid1.compartment.oc1..aaaaaaaagf5galz7jxth4ef6m6zxk4lsvigpv7t7kox7gpthsp2irycptmka";
        String authToken = "}uC6hEgYpJT-)]7UV7cb";
        String tenancyName = "weblogicondocker";
        String username = "paulparkinson";
        String streamPoolId = "ocid1.streampool.oc1.phx.amaaaaaagu6anria6zgrt4esdg6aibvi6zshwumji4nlb5735egsx5sfc5ea";
        String topicName = "helidon-oss-kafka";

        Properties properties = new Properties();
        properties.put("bootstrap.servers", "streaming.us-phoenix-1.oci.oraclecloud.com:9092");
        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                        + tenancyName + "/"
                        + username + "/"
                        + streamPoolId + "\" "
                        + "password=\""
                        + authToken + "\";"
        );

        properties.put("retries", 5); // retries on transient errors and load balancing disconnection
        properties.put("max.request.size", 1024 * 1024); // limit request size to 1MB

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
        Map<String, List<PartitionInfo>> topics = consumer.listTopics();
        System.out.println("CompatibleProducer.produce consumer.listTopics():" + topics);
        Iterator<Map.Entry<String, List<PartitionInfo>>> iterator = topics.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, List<PartitionInfo>> next = iterator.next();
            System.out.println("CompatibleProducer.produce next:" + next);
            System.out.println("CompatibleProducer.produce topic:" + next.getValue() + ":" + topics.get(next));
        }
        consumer.close();

        KafkaProducer producer = new KafkaProducer<>(properties);
        for (int i = 0; i < 5; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topicName, UUID.randomUUID().toString(), "Test record #" + i);

            System.out.println("CompatibleProducer.produce record:" + record);
            producer.send(record, (md, ex) -> {
                if( ex != null ) {
                    ex.printStackTrace();
                }
                else {
                    System.out.println(
                            "Sent msg to "
                                    + md.partition()
                                    + " with offset "
                                    + md.offset()
                                    + " at "
                                    + md.timestamp()
                    );
                }
            });
        }
        producer.flush();
        producer.close();
        System.out.println("produced 5 messages");
    }

}