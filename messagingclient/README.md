# Helidon Messaging (Client) Proposal

Provide an API that enables reactive messaging support to be used with Helidon SE.

## Proposal

The Helidon Messaging Client is an abstraction layer over
- message listening
- handling of results
- message publishing

The Helidon Messaging Client also supports
- the above three (listening, handling, publishing) combined forming the fundamentals of event sourcing and CQRS
- this combination using AQ in order to take advantage of atomicity provided via use of local transaction for all three operations
- this combination also supporting Kafka, Debezium, etc. 
- event sourcing to facilitate Sagas

The Helidon Messaging Client is related to
- An implementation of Eclipse MicroProfile Reactive Messaging specification, although it does attempt to provide much of the same purpose (sans CDI, etc. of course) and we may want to consider compatibility here to be on par with Quarkus and support debezium etc. streams // todo if we can just say it's completely compatibile but without CDI  (like metrics and healthchecks and openapi)
- Providing a MicroProfile Reactive Streams Operator, although this might be something to consider as we would essentially be doing the same and none exists for AQ (or any JMS) currently (SmallRye supports Kafka, AMQP 1.0, MQTT).


### Required features

1. The API must be reactive 
2. There must be support for configuring Tracing without dependency on Tracing API
3. There must be support for configuring Metrics without dependency on Metrics API
4. There must be support for configuring Healthchecks without dependency on Healthcheck API
5. The first implementation must work (in priority order) 1. Kafka and 2. JMS or at 3. Oracle AQ (JMS)
  
### API

The API main interfaces/classes:
- `HelidonMessaging` - the entry point to create an instance of a Helidon Messaging, uses the usual `Builder`/`Config` pattern
    the provider to be used is either explicitly configured, or defined by name (see `MessageProvider` below) or
    the first available one is used (ordered by priority). The instance has two methods to execute operations -
    `execute` and `inTransaction`
- `MessagingException` a runtime exception to use when something fails (wrap underlying exception for system such as `javax.jms.JMSException`)

public interface Session {
    SendingMessage createMessage();
    send(Message message);
    Connection getConnection(); //todo open issue
}

public static interface SendingMessage { //todo name is odd, rename Outgoing Outbound - reuse of MP whenver possible
    addHeader(String, String);
    writeContent(Object anObject);
}

public static interface Message {
    Map<String, Object> headers();
    T as(Class<T> type);
    T as(GenericType<T> type);
}

### SPI

SPI is used to add support for additional messaging systems (such as AQ, Kafka etc.).
The SPI Classes:
- `MessagingProvider` - a Java Service loader interface used to locate available implementations
- `MessagingProviderBuilder` - builder used to configure the underlying messaging system and behavior of the 
    implementation (such as listeners, sessions, etc.)
   

## Possible Implementations

Our plan is to provide Helidon Messaging API implementations as very thin layers over several existing messaging APIs which are not reactive or their API is unnecessarily complex or does not match Helidon style of API:
- JMS
- JMS (specifically AQ)
- Kafka (including debezium)
- others (those supported by smallrye listed here: https://smallrye.io/smallrye-reactive-messaging/)

## Configuration

There is a single configuration option expected when loading the `HelidonMessaging` from configuration - 
`source`. If defined, it is used to locate the `MessagingProvider` with the same name. If undefined, the
first provider is used (ordered by priority).

Each messaging implementation may choose what is required in configuration

### Example
The following Yaml file configures JMS factory to AQ:
```yaml
kafka-messaging:
    source: aq
    url: jdbc:oracle:thin:@//129.146.63.193:1521/testpdb.auranet0.vcndns.oraclevcn.com
    username: aq######
    password: AQ############
    listeners:
      demoqueue: "demoqueue"
      selector: "(releaseYear < 1980) OR (releaseYear > 1989)"
aq-messaging:
```

This configuration file defines both connectivity and listeners.

### Messaging Initialization:
```java
Messaging messaging = Messaging.create(config.get("kafka-messaging")
        .orElseThrow(() -> new IllegalStateException("Configuration is missing"));
```

### Message Listener Execution:

#### Add Listener
```java
messaging.addListener((session, message) -> processMessage(session, message));
```
## Open questions

processMessage conducts the processing of the message which potentially includes:
1. Processing data where, in the case of AQ, the connection is obtained from the underlying AQJmsSession, therefore:
    - take into account when using the DBClient, connections are obtained ConnectionPool.builder()....build();
    - take into account if not using DBClient approach session is passed into processMessage for JDBC connection extraction.
    - ie resolve the design for the above two items
    AQConnectionPool that takes AQSession eg
2. Sending a message/event (see Session and SendingMessage, equivalent of @Incoming / @Outgoing but outgoing is in processMessage) 

processMessage is the business logic
completionstage maybe for outgoingmessage
@Incoming
@Outgoing
method message processMessage() {
do db work
return message
}
