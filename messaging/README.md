# Helidon Messaging (Client) Proposal

Provide an API that enables messaging support to be used with Helidon SE.

## Proposal

The Helidon Messaging Client is an abstraction layer over
- Message listening
- Processing of results
- Message publishing

The Helidon Messaging Client also supports
- The above three (listening, processing, publishing) combined forming the fundamentals of event sourcing.
- Support for Kafka and other major messaging products.
- Support for AQ in order to take advantage of atomicity provided via use of local transaction.
- Event sourcing to facilitate Sagas.

The Helidon Messaging Client is related to
- An implementation of Eclipse MicroProfile Reactive Messaging. 
- An implementation of Eclipse MicroProfile Reactive Streams Operator - Daniel working on this.


### Required features

1. There must be support reactive messaging
2. There must be support for configuring Tracing without dependency on Tracing API
3. There must be support for configuring Metrics without dependency on Metrics API
4. There must be support for configuring Healthchecks without dependency on Healthcheck API
5. The first implementation must support (in priority order) 1. Kafka and 2. JMS including AQ
  
### API:
````
// Messaging client creation...
public static MessagingClient build(Config config)

public class MessagingClient {

    /**
     * Sets up listener for incoming messages on specified channel.
     * @param incomingMessagingService IncomingMessagingService implementation provided is executed when message is received.
     * @param channelname Name of channel to listen on.
     * @param acknowledgement org.eclipse.microprofile.reactive.messaging.Acknowledgement
     */
    public void incoming(IncomingMessagingService incomingMessagingService, String channelname,
                         Acknowledgment.Strategy acknowledgement)

    /**
     * Sends message on specified channel.
     * @param outgoingMessagingService OutgoingMessagingService implementation provided is executed when message is sent.
     * @param channelname Name of channel to send on.
     */
    public void outgoing(OutgoingMessagingService outgoingMessagingService, String channelname)

   /**
     * Sets up listener for incoming messages on specified channel and in reaction sends message on specified channel.
     * @param processingMessagingService ProcessingMessagingService implementation provided is executed when
     *                                   when message is received and before message is sent.
     * @param incomingchannelname Name of channel to listen on.
     * @param outgoingchannelname Name of channel to send on.
     * @param acknowledgement org.eclipse.microprofile.reactive.messaging.Acknowledgement
     */
    public void incomingoutgoing(ProcessingMessagingService processingMessagingService, String incomingchannelname,
                                 String outgoingchannelname, Acknowledgment.Strategy acknowledgement)

}
                    
    /**
     * Implementation receives message and associated session and connection (if/as appropriate) whenever a message
     * is received.
     */                   
     public interface IncomingMessagingService {
        void onIncoming(Message message, Connection connection, Session session);
     }

     /**
      * Implementation receives session and connection (if/as appropriate) and returns the Message to be sent.
     */
     public interface OutgoingMessagingService {
        Message onOutgoing(Connection connection, Session session) throws Exception; 
     }

     /**
      * Implementation receives message and associated session and connection (if/as appropriate) whenever a message
      * is received and returns the Message to be sent. All operations are conducted within the same session.
     */
     public interface ProcessingMessagingService {
        Message onProcessing(Message message, Connection connection, Session session) throws Exception;
     }
````
### SPI

SPI is used to add support for additional messaging systems.

The SPI follows the MicroProfile Reactive Messaging specification and so, for example, users can provide additional

Connectors (`IncomingConnectorFactory` and `OutgoingConnectorFactory` implementations) and easily plug them in via config.
   

## Implementation

Our plan is to provide a Helidon Messaging API as very thin layers over several existing messaging APIs 
which are not reactive or their API is unnecessarily complex or does not match Helidon style of API. 
The configuration will be portable with the MicroProfile Reactive Messaging and the API will follow a similar convention
to the annotations in MP Reactive Messaging with a few enhancements.
- Kafka 
- JMS (specifically AQ)
- others (Oracle Streaming Service, perhaps debezium, and those supported by smallrye listed here: https://smallrye.io/smallrye-reactive-messaging/)

## Configuration

Both MP and SE configuration will be supported and will follow the MicroProfile Reactive Messaging convention.
"mp.messaging.connector.[connectorname].classname" will be used in place of the @Connector annotation.

### Example
# example for connector
                mp.messaging.connector.kafka.classname=io.helidon.messaging.kafka.connector.KafkaConnector
                mp.messaging.connector.aqjms.classname=io.helidon.messaging.jms.connector.JMSConnector
# example for incoming and outgoing channels
                mp.messaging.incoming.kafkafoochannelname.connector=kafka
                mp.messaging.incoming.kafkafoochannelname.url=jdbc:oracle:thin:@//127.0.0.1:1521/testpdb.oraclevcn.com
                mp.messaging.incoming.kafkafoochannelname.user=scott
                mp.messaging.incoming.kafkafoochannelname.password=tiger
                mp.messaging.incoming.kafkafoochannelname.queue=fooqueue
                mp.messaging.incoming.kafkafoochannelname.selector="foomessageproperty = barmessagepropertyvalue'"
                mp.messaging.outgoing.aqjmsfoochannelname.connector=aqjms
                mp.messaging.outgoing.aqjmsfoochannelname.url=jdbc:oracle:thin:@//127.0.0.1:1521/testpdb.oraclevcn.com
                mp.messaging.outgoing.aqjmsfoochannelname.user=scott
                mp.messaging.outgoing.aqjmsfoochannelname.password=tiger
                mp.messaging.outgoing.aqjmsfoochannelname.queue=fooqueue
                mp.messaging.outgoing.aqjmsfoochannelname.selector="foomessageproperty = barmessagepropertyvalue'"
todo equivalent SE config example...
```yaml
incoming.aqjmsfoochannelname:
    connector: aq
    url: jdbc:oracle:thin:@//127.0.0.1:1521/testpdb.oraclevcn.com
    username: scott
    password: tiger
    queue: fooqueue
    selector: "foomessageproperty = barmessagepropertyvalue'"
```

This configuration file defines both connectivity and listeners.


### Messaging client creation :
```java
MessagingClient messagingClient = MessagingClient.build(config.get("incoming.aqjmsfoochannelname")
        .orElseThrow(() -> new IllegalStateException("Configuration is missing"));
```

### Incoming messaging:
```java
//AQ JMS example...
messagingClient.incoming((message, connection, session) -> {
                    MessageWithConnectionAndSession messageWithConnectionAndSession = 
                                 (MessageWithConnectionAndSession) message.unwrap(Message.class);
                    Message jmsMessage = messageWithConnectionAndSession.getPayload();
                    String foomessageproperty = jmsMessage.getStringProperty("foomessageproperty");
                    connection.createStatement().execute("some sql with foomessageproperty value");
                }, "somechannelname", Acknowledgment.Strategy.NONE);
```


### Outgoing messaging:
```java
//Kafka example...
messagingClient.outgoing((connection, session) -> new JMSMessage() {
            @Override
            public ProducerRecord<K, V> unwrap(Class unwrapType) {
                    return new ProducerRecord((String)properties.get("topic"), "test");;
            }
        }, "somechannelname");
//AQ JMS example (event sourcing)... 
messagingClient.outgoing((connection, session) -> new KafkaMessage() {
            @Override
            public javax.jms.Message unwrap(Class unwrapType) {
                    connection.createStatement().execute("some sql with barmessagepropertyvalue");
                    TextMessage textMessage = session.createTextMessage("some message");
                    textMessage.setStringProperty("foomessageproperty", "barmessagepropertyvalue");
                    return textMessage;
            }
        }, "somechannelname");
```


### IncomingOutgoing/Processing messaging:
```java
//AQ JMS example...
messagingClient.incomingoutgoing( (message, connection, session) -> {
            MessageWithConnectionAndSession messageWithConnectionAndSession =
                    (MessageWithConnectionAndSession) message.unwrap(Message.class);
            Message jmsMessage = messageWithConnectionAndSession.getPayload();
            String foomessagepropertyvalue = jmsMessage.getStringProperty("foomessageproperty");
            connection.createStatement().execute("some sql with foomessagepropertyvalue");
            return JMSMessage(session, foomessageproperty);
        }, "someincomingchannelname", "someoutgoingchannelname", Acknowledgment.Strategy.NONE);
```
## Open questions

Use opentracing contrib that exist for Kafka and JMS? 

Proceed to implement MicroProfile Reactive Messaging in MP? 
Currently we plan to wait and see. One advantage of the above approach/implementation, once complete, is that providing the MP feature would not take much effort if it's deemed worth it. 
