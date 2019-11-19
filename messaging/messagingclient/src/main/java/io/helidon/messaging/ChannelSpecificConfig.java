package io.helidon.messaging;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.Optional;

public class ChannelSpecificConfig implements Config {
    HashMap<String, String> values = new HashMap();

    public ChannelSpecificConfig(Config config, String channelname, String connfactoryPrefix) {
        for (String propertyName : config.getPropertyNames()) {
            if(propertyName.startsWith(connfactoryPrefix) ) {
                String striped = propertyName.substring(connfactoryPrefix.length());
                if(striped.substring(0, striped.indexOf(".")).equals(channelname))
                    values.put(propertyName, config.getValue(propertyName, String.class));
            }
        }
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return (T) values.get(propertyName);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return Optional.empty();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return values.keySet();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return null;
    }
}
