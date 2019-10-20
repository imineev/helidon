package io.helidon.messagingclient.kafka;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.config.Config;
import io.helidon.messagingclient.MessagingInterceptor;
import io.helidon.messagingclient.MessagingClient;
import io.helidon.messagingclient.MessagingOperationType;
import io.helidon.messagingclient.MessagingOperations;
import io.helidon.messagingclient.common.InterceptorSupport;
import io.helidon.messagingclient.spi.MessagingClientProviderBuilder;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class KafkaMessagingClientProviderBuilder implements MessagingClientProviderBuilder<KafkaMessagingClientProviderBuilder> {

    private final InterceptorSupport.Builder interceptors = InterceptorSupport.builder();
    private String url;
    private String username;
    private String password;
    private String topic;
    private String queue;
    private String bootstrapservers;
    private int numberofmessagestoconsume;
    KafkaMessagingClientConfig config;
    private Supplier<ExecutorService> executorService;
    private MessagingOperations operations;

    public InterceptorSupport interceptors() {
        return interceptors.build();
    }

    public KafkaMessagingClientConfig messagingConfig() {
        return config = new KafkaMessagingClientConfig(
                url, username, password, topic, queue, bootstrapservers, numberofmessagestoconsume);
    }

    ExecutorService executorService() {
        return executorService.get();
    }

    @Override
    public KafkaMessagingClientProviderBuilder url(String url){
        this.url = url;
        return this;
    }

    @Override
    public KafkaMessagingClientProviderBuilder username(String username)
        {
            this.username = username;
            return this;
        }

    @Override
    public KafkaMessagingClientProviderBuilder password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public KafkaMessagingClientProviderBuilder addInterceptor(MessagingInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    @Override
    public KafkaMessagingClientProviderBuilder addInterceptor(MessagingInterceptor interceptor, MessagingOperationType... operationNames) {
        this.interceptors.add(interceptor, operationNames);
        return this;
    }

    @Override
    public KafkaMessagingClientProviderBuilder addInterceptor(MessagingInterceptor interceptor, String... operationNames) {
        this.interceptors.add(interceptor, operationNames);
        return this;
    }

    @Override
    public MessagingClient build() {
        if (null == executorService) {
            executorService = ThreadPoolSupplier.create();
        }
        return new KafkaMessagingClient(this);
    }

    @Override
    public void topic(String s) {
        topic = s;
    }

    @Override
    public void queue(String s) {
        queue = s;
    }

    public KafkaMessagingClientProviderBuilder executorService(Supplier<ExecutorService> executorServiceSupplier) {
        this.executorService = executorServiceSupplier;
        return this;
    }

    @Override
    public KafkaMessagingClientProviderBuilder config(Config config) {
        config.get("url").asString().ifPresent(this::url);
        config.get("username").asString().ifPresent(this::username);
        config.get("password").asString().ifPresent(this::password);
        config.get("topic").asString().ifPresent(this::topic);
        config.get("queue").asString().ifPresent(this::queue);
        config.get("bootstrap.servers").asString().ifPresent(this::bootstrapservers);
        config.get("numberofmessagestoconsume").asInt().ifPresent(this::numberofmessagestoconsume);
        config.get("operations").as(MessagingOperations::create).ifPresent(this::operations);
        config.get("executor-service").as(ThreadPoolSupplier::create).ifPresent(this::executorService);
        // todo set the connpool here at least for jdbc
        return this;
    }

    private void bootstrapservers(String s) {
        bootstrapservers = s;
    }

    private void numberofmessagestoconsume(int s) {
        numberofmessagestoconsume = s;
    }

    MessagingOperations operations() {
        return operations;
    }

    public KafkaMessagingClientProviderBuilder operations(MessagingOperations operations) {
        this.operations = operations;
        return this;
    }

}
