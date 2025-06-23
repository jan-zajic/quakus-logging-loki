package io.quarkus.logging.loki;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

/**
 * Configuration for Sentry logging.
 */
@ConfigMapping(prefix = "quarkus.log.handler.loki")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LokiConfig {

    static final Pattern LABEL_NAME_PATTER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * enabled
     */
    @WithDefault("false")
    public boolean enabled();

    /**
     * named handler
     */
    Optional<String> name();

    /**
     * log endpoint path
     */
    public Optional<String> logEndpoint();

    /**
     * host
     */
    public Optional<String> host();

    /**
     * port
     */
    @WithDefault("3100")
    public int port();

    /**
     * useSSL
     */
    @WithDefault("false")
    public boolean useSSL();

    /**
     * url
     */
    @WithDefault( "false")
    public boolean useDaemonThreads();

    /**
     * username
     */
    public Optional<String> username();

    /**
     * password
     */
    public Optional<String> password();

    /**
     * connectTimeoutMillis
     */
    @WithDefault( "5000")
    public int connectTimeoutMillis();

    /**
     * url
     */
    @WithDefault( "60000")
    public int readTimeoutMillis();

    /**
     * url
     */
    @WithDefault( "3")
    public int maxRetries();

    /**
     * url
     */
    @WithDefault( "32")
    public int bufferSizeMegabytes();

    /**
     * url
     */
    @WithDefault( "true")
    public boolean useOffHeapBuffer();

    /**
     * url
     */
    @WithDefault( "102400")
    public long batchSize();

    /**
     * url
     */
    @WithDefault( "5")
    public long batchWait();

    /**
     * url
     */
    @WithDefault( "10")
    public long logShipperWakeupIntervalMillis();

    /**
     * url
     */
    @WithDefault( "10")
    public int shutdownTimeoutSeconds();

    /**
     * url
     */
    @WithDefault( "10")
    public int maxLogLineSizeKilobytes();

    /**
     * url
     */
    @WithDefault( "100")
    public int maxRequestsInFlight();

    /**
     * Headers
     */
    Map<String, String> headers();

    /**
     * url
     */
    Map<String, String> labels();

}
