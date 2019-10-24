package io.helidon.messagingclient.jms;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.config.Config;
import io.helidon.messagingclient.MessagingClient;
import io.helidon.messagingclient.MessagingInterceptor;
import io.helidon.messagingclient.MessagingChannelType;
import io.helidon.messagingclient.MessagingChannels;
import io.helidon.messagingclient.common.InterceptorSupport;
import io.helidon.messagingclient.spi.MessagingClientProviderBuilder;


import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class JMSMessagingClientProviderBuilder implements MessagingClientProviderBuilder<JMSMessagingClientProviderBuilder> {

    private final InterceptorSupport.Builder interceptors = InterceptorSupport.builder();
    private String url;
    private String username;
    private String password;
    private ConnectionPool connectionPool;
    private String topic;
    private String queue;
    private String bootstrapservers;
    private int numberofmessagestoconsume;
    JMSMessagingClientConfig config;
    private Supplier<ExecutorService> executorService;
    private MessagingChannels channels;

    public InterceptorSupport interceptors() {
        return interceptors.build();
    }

    public JMSMessagingClientConfig messagingConfig() {
        return config = new JMSMessagingClientConfig(
                url, username, password, topic, queue, bootstrapservers, numberofmessagestoconsume);
    }

    ExecutorService executorService() {
        return executorService.get();
    }

    @Override
    public JMSMessagingClientProviderBuilder url(String url){
        this.url = url;
        return this;
    }

    @Override
    public JMSMessagingClientProviderBuilder username(String username)
        {
            this.username = username;
            return this;
        }

    @Override
    public JMSMessagingClientProviderBuilder password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public JMSMessagingClientProviderBuilder addInterceptor(MessagingInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    @Override
    public JMSMessagingClientProviderBuilder addInterceptor(MessagingInterceptor interceptor, MessagingChannelType... channelNames) {
        this.interceptors.add(interceptor, channelNames);
        return this;
    }

    @Override
    public JMSMessagingClientProviderBuilder addInterceptor(MessagingInterceptor interceptor, String... channelNames) {
        this.interceptors.add(interceptor, channelNames);
        return this;
    }

    @Override
    public MessagingClient build() {
        if (null == executorService) {
            executorService = ThreadPoolSupplier.create();
        }
        connectionPool = ConnectionPool.builder()
                .url(url)
                .username(username)
                .password(password)
                .build();
        return new JMSMessagingClient(this);
    }

    @Override
    public void topic(String s) {
        topic = s;
    }

    @Override
    public void queue(String s) {
        queue = s;
    }

    public JMSMessagingClientProviderBuilder executorService(Supplier<ExecutorService> executorServiceSupplier) {
        this.executorService = executorServiceSupplier;
        return this;
    }

    @Override
    public JMSMessagingClientProviderBuilder config(Config config) {
        config.get("url").asString().ifPresent(this::url);
        config.get("username").asString().ifPresent(this::username);
        config.get("password").asString().ifPresent(this::password);
        config.get("topic").asString().ifPresent(this::topic);
        config.get("queue").asString().ifPresent(this::queue);
        config.get("bootstrap.servers").asString().ifPresent(this::bootstrapservers);
        config.get("numberofmessagestoconsume").asInt().ifPresent(this::numberofmessagestoconsume);
        config.get("channels").as(MessagingChannels::create).ifPresent(this::channels);
        config.get("executor-service").as(ThreadPoolSupplier::create).ifPresent(this::executorService);
        return this;
    }

    private void bootstrapservers(String s) {
        bootstrapservers = s;
    }

    private void numberofmessagestoconsume(int s) {
        numberofmessagestoconsume = s;
    }

    MessagingChannels channels() {
        return channels;
    }

    public JMSMessagingClientProviderBuilder channels(MessagingChannels channels) {
        this.channels = channels;
        return this;
    }

    public ConnectionPool connectionpool() {
        return connectionPool;
    }
}
