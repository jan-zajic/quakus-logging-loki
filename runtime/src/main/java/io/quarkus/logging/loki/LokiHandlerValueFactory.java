package io.quarkus.logging.loki;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziInitializer;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.MutableMonitoringModuleWrapper;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.util.stream.Stream;

@Recorder
public class LokiHandlerValueFactory {

    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;
    private static final int BYTES_IN_KILOBYTE = 1024;

    Logger LOGGER = LoggerFactory.getLogger(LokiHandlerValueFactory.class);

    public RuntimeValue<Optional<Handler>> create(final LokiConfig config) {
        if(!config.enabled()) {
            return  new RuntimeValue<>(Optional.empty());
        }
        ClientConfiguration configurationBuilder = ClientConfiguration.builder()
                .withLogEndpoint(config.logEndpoint().orElse(null))
                .withHost(config.host().get())
                .withPort(config.port())
                .withUseSSL(config.useSSL())
                .withUsername(config.username().orElse(null))
                .withPassword(config.password().orElse(null))
                .withConnectionTimeoutMillis(config.connectTimeoutMillis())
                .withMaxRetries(config.maxRetries())
                .withRequestTimeoutMillis(config.readTimeoutMillis())
                .withMaxRequestsInFlight(config.maxRequestsInFlight())
                .build();

        String[] additionalHeaders = config.headers().entrySet().stream()
                .flatMap(header -> Stream.of(header.getKey(), header.getValue()))
                .toArray(String[]::new);

        MutableMonitoringModuleWrapper monitoringModuleWrapper = new MutableMonitoringModuleWrapper();
        monitoringModuleWrapper.setMonitoringModule(new StandardMonitoringModule());

        NettyHttpClient httpClient = HttpClientFactory
                .defaultFactory()
                .getHttpClient(
                        configurationBuilder,
                        monitoringModuleWrapper,
                        additionalHeaders
                );

        int bufferSizeBytes = config.bufferSizeMegabytes() * BYTES_IN_MEGABYTE;
        if (!TjahziInitializer.isCorrectSize(bufferSizeBytes)) {
            LOGGER.warn("Invalid log buffer size {} - using nearest power of two greater than provided value, no less than 1MB. {}",
                    bufferSizeBytes,
                    "Check out documentation at https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing."
            );
        }

        LoggingSystem loggingSystem = new TjahziInitializer().createLoggingSystem(
                httpClient,
                monitoringModuleWrapper,
                config.labels(),
                config.batchSize(),
                TimeUnit.SECONDS.toMillis(config.batchWait()),
                bufferSizeBytes,
                config.logShipperWakeupIntervalMillis(),
                TimeUnit.SECONDS.toMillis(config.shutdownTimeoutSeconds()),
                config.useOffHeapBuffer(),
                config.useDaemonThreads()
        );

        LokiHandler handler = new LokiHandler(loggingSystem);
        return new RuntimeValue<>(Optional.of(handler));
    }
}
