package io.quarkus.logging.loki;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mjaron.tinyloki.Labels;
import pl.mjaron.tinyloki.Settings;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;

@Recorder
public class LokiHandlerValueFactory {

    Logger LOGGER = LoggerFactory.getLogger(LokiHandlerValueFactory.class);

    public RuntimeValue<Optional<Handler>> create(final LokiConfig config) {
        if(!config.enabled()) {
            return  new RuntimeValue<>(Optional.empty());
        }
        var url = (config.useSSL() ? "https" : "http") + "://" +config.host().get()+":"+config.port();
        if(config.logEndpoint().isPresent()) {
            url += config.logEndpoint().get();
        }
        Settings lokiSettings = Settings.fromUrl(url)
                .withConnectTimeout(config.connectTimeoutMillis());
        if(config.username().isPresent()) {
            lokiSettings = lokiSettings.withBasicAuth(config.username().get(), config.password().orElse(null));
        }

        Labels labels = Labels.of();
        for (Map.Entry<String, String> e : config.labels().entrySet()) {
            if(!e.getValue().startsWith("$")) {
                labels.l(e.getKey(), e.getValue());
            }
        }
        if(!labels.isEmpty()) {
            lokiSettings = lokiSettings.withLabels(labels);
        }

        LokiHandler handler = new LokiHandler(lokiSettings, config);
        return new RuntimeValue<>(Optional.of(handler));
    }
}
