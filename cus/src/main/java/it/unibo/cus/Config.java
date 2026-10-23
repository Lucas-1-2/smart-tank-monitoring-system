package it.unibo.cus;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Central configuration of the Control Unit Subsystem. */
public final class Config {

    private static final String CONFIG_FILE_NAME = "cus.properties";

    /* Defaults for values that can be customized through cus.properties. */
    private static final double DEFAULT_L1 = 40.0;
    private static final double DEFAULT_L2 = 70.0;
    private static final long DEFAULT_T1_MS = 5_000L;
    private static final long DEFAULT_T2_MS = 10_000L;

    private static final String DEFAULT_HTTP_HOST = "0.0.0.0";
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final String DEFAULT_DBS_DIRECTORY = "../dbs";

    private static final boolean DEFAULT_SERIAL_ENABLED = true;
    private static final int DEFAULT_SERIAL_BAUD_RATE = 115_200;

    private static final boolean DEFAULT_MQTT_ENABLED = true;
    private static final String DEFAULT_MQTT_BROKER_URI =
        "tcp://broker.mqtt-dashboard.com:1883";
    private static final String DEFAULT_MQTT_TOPIC =
        "smart-tank/sam-lucas/tms/data";

    /* Configurable values. Defaults are used until load() reads cus.properties. */
    public static double L1 = DEFAULT_L1;
    public static double L2 = DEFAULT_L2;
    public static long T1_MS = DEFAULT_T1_MS;
    public static long T2_MS = DEFAULT_T2_MS;

    public static String HTTP_HOST = DEFAULT_HTTP_HOST;
    public static int HTTP_PORT = DEFAULT_HTTP_PORT;
    public static String DBS_DIRECTORY = DEFAULT_DBS_DIRECTORY;

    public static boolean SERIAL_ENABLED = DEFAULT_SERIAL_ENABLED;
    public static String SERIAL_PORT = null;
    public static int SERIAL_BAUD_RATE = DEFAULT_SERIAL_BAUD_RATE;

    public static boolean MQTT_ENABLED = DEFAULT_MQTT_ENABLED;
    public static String MQTT_BROKER_URI = DEFAULT_MQTT_BROKER_URI;
    public static String MQTT_TOPIC = DEFAULT_MQTT_TOPIC;

    /* Internal parameters that do not need external configuration. */
    public static final int HISTORY_SIZE = 20;
    public static final long CONTROL_TICK_MS = 100L;
    public static final int MQTT_QOS = 0;
    public static final long MQTT_RETRY_DELAY_MS = 5_000L;

    private static boolean loaded;

    private Config() {
    }

    /**
     * Loads cus.properties from the current working directory once.
     * Missing files or properties simply keep the defaults above.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }

        final Path configPath = Path.of(CONFIG_FILE_NAME);
        if (!Files.exists(configPath)) {
            loaded = true;
            System.out.println(
                "[CONFIG] " + CONFIG_FILE_NAME + " not found; using default values."
            );
            return;
        }

        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (final IOException exception) {
            throw new IllegalStateException(
                "Cannot read " + configPath.toAbsolutePath(),
                exception
            );
        }

        try {
            L1 = doubleProperty(properties, "cus.l1", DEFAULT_L1);
            L2 = doubleProperty(properties, "cus.l2", DEFAULT_L2);
            T1_MS = longProperty(properties, "cus.t1.ms", DEFAULT_T1_MS);
            T2_MS = longProperty(properties, "cus.t2.ms", DEFAULT_T2_MS);

            HTTP_HOST = stringProperty(properties, "cus.http.host", DEFAULT_HTTP_HOST);
            HTTP_PORT = intProperty(properties, "cus.http.port", DEFAULT_HTTP_PORT);
            DBS_DIRECTORY = stringProperty(
                properties,
                "cus.dbs.dir",
                DEFAULT_DBS_DIRECTORY
            );

            SERIAL_ENABLED = booleanProperty(
                properties,
                "cus.serial.enabled",
                DEFAULT_SERIAL_ENABLED
            );
            SERIAL_PORT = optionalStringProperty(properties, "cus.serial.port");
            SERIAL_BAUD_RATE = intProperty(
                properties,
                "cus.serial.baud",
                DEFAULT_SERIAL_BAUD_RATE
            );

            MQTT_ENABLED = booleanProperty(
                properties,
                "cus.mqtt.enabled",
                DEFAULT_MQTT_ENABLED
            );
            MQTT_BROKER_URI = stringProperty(
                properties,
                "cus.mqtt.broker",
                DEFAULT_MQTT_BROKER_URI
            );
            MQTT_TOPIC = stringProperty(
                properties,
                "cus.mqtt.topic",
                DEFAULT_MQTT_TOPIC
            );
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Invalid value in " + configPath.toAbsolutePath() + ": "
                    + exception.getMessage(),
                exception
            );
        }

        loaded = true;
        System.out.println("[CONFIG] Loaded " + configPath.toAbsolutePath());
    }

    /** Loads and validates configuration before the application starts. */
    public static void validate() {
        load();

        require(L1 >= 0.0 && L1 <= 100.0, "L1 must be in [0,100]");
        require(L2 >= 0.0 && L2 <= 100.0, "L2 must be in [0,100]");
        require(L1 < L2, "L1 must be lower than L2");
        require(T1_MS > 0, "T1 must be > 0");
        require(T2_MS > 0, "T2 must be > 0");
        require(HISTORY_SIZE > 0, "History size must be > 0");
        require(CONTROL_TICK_MS > 0, "Control tick must be > 0");
        require(HTTP_PORT > 0 && HTTP_PORT <= 65_535, "HTTP port must be valid");
        require(!HTTP_HOST.isBlank(), "HTTP host cannot be blank");
        require(!DBS_DIRECTORY.isBlank(), "DBS directory cannot be blank");

        if (SERIAL_ENABLED) {
            require(SERIAL_BAUD_RATE > 0, "Serial baud rate must be > 0");
        }

        if (MQTT_ENABLED) {
            require(!MQTT_BROKER_URI.isBlank(), "MQTT broker cannot be blank");
            require(!MQTT_TOPIC.isBlank(), "MQTT topic cannot be blank");
            require(MQTT_QOS >= 0 && MQTT_QOS <= 2, "MQTT QoS must be 0, 1 or 2");
            require(MQTT_RETRY_DELAY_MS > 0, "MQTT retry delay must be > 0");
        }
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalStateException("Invalid CUS configuration: " + message);
        }
    }

    private static String stringProperty(
        final Properties properties,
        final String key,
        final String defaultValue
    ) {
        final String value = properties.getProperty(key);
        return value == null ? defaultValue : value.trim();
    }

    private static String optionalStringProperty(
        final Properties properties,
        final String key
    ) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int intProperty(
        final Properties properties,
        final String key,
        final int defaultValue
    ) {
        final String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException(
                key + " must be an integer, found: " + value,
                exception
            );
        }
    }

    private static long longProperty(
        final Properties properties,
        final String key,
        final long defaultValue
    ) {
        final String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException(
                key + " must be an integer, found: " + value,
                exception
            );
        }
    }

    private static double doubleProperty(
        final Properties properties,
        final String key,
        final double defaultValue
    ) {
        final String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException(
                key + " must be a number, found: " + value,
                exception
            );
        }
    }

    private static boolean booleanProperty(
        final Properties properties,
        final String key,
        final boolean defaultValue
    ) {
        final String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        final String normalized = value.trim();
        if (normalized.equalsIgnoreCase("true")) {
            return true;
        }
        if (normalized.equalsIgnoreCase("false")) {
            return false;
        }

        throw new IllegalArgumentException(
            key + " must be true or false, found: " + value
        );
    }
}
