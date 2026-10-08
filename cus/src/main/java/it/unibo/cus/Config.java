package it.unibo.cus;

/**
 * Central configuration of the Control Unit Subsystem.
 *
 * Values can be overridden with Java system properties, for example:
 * -Dcus.serial.port=COM5
 * -Dcus.mqtt.topic=unibo/esiot/2025-2026/group06/tms/data
 * -Dcus.dbs.dir=../dbs
 */
public final class Config {

    private Config() {
    }

    /* Automatic policy parameters. */
    public static final double L1 = doubleSetting("cus.l1", "CUS_L1", 40.0);
    public static final double L2 = doubleSetting("cus.l2", "CUS_L2", 70.0);
    public static final long T1_MS = longSetting("cus.t1.ms", "CUS_T1_MS", 5_000L);
    public static final long T2_MS = longSetting("cus.t2.ms", "CUS_T2_MS", 10_000L);

    /* Number of measurements retained for DBS. */
    public static final int HISTORY_SIZE = intSetting("cus.history.size", "CUS_HISTORY_SIZE", 20);

    /* Main periodic check for T1/T2. */
    public static final long CONTROL_TICK_MS = longSetting(
        "cus.control.tick.ms",
        "CUS_CONTROL_TICK_MS",
        100L
    );

    /* HTTP / DBS. */
    public static final String HTTP_HOST = stringSetting("cus.http.host", "CUS_HTTP_HOST", "0.0.0.0");
    public static final int HTTP_PORT = intSetting("cus.http.port", "CUS_HTTP_PORT", 8080);
    public static final String DBS_DIRECTORY = stringSetting("cus.dbs.dir", "CUS_DBS_DIR", "../dbs");

    /* WCS serial line. */
    public static final boolean SERIAL_ENABLED = booleanSetting(
        "cus.serial.enabled",
        "CUS_SERIAL_ENABLED",
        true
    );
    public static final String SERIAL_PORT = stringSetting("cus.serial.port", "CUS_SERIAL_PORT", "COM7");
    public static final int SERIAL_BAUD_RATE = intSetting(
        "cus.serial.baud",
        "CUS_SERIAL_BAUD",
        115200
    );

    /* TMS MQTT configuration. */
    public static final boolean MQTT_ENABLED = booleanSetting(
        "cus.mqtt.enabled",
        "CUS_MQTT_ENABLED",
        true
    );
    public static final String MQTT_BROKER_URI = stringSetting(
        "cus.mqtt.broker",
        "CUS_MQTT_BROKER",
        "tcp://broker.mqtt-dashboard.com:1883"
    );
    public static final String MQTT_TOPIC = stringSetting(
        "cus.mqtt.topic",
        "CUS_MQTT_TOPIC",
        "unibo/esiot/2025-2026/group06/tms/data"
    );
    public static final int MQTT_QOS = intSetting("cus.mqtt.qos", "CUS_MQTT_QOS", 0);
    public static final long MQTT_RETRY_DELAY_MS = longSetting(
        "cus.mqtt.retry.ms",
        "CUS_MQTT_RETRY_MS",
        5_000L
    );

    /** Validates configuration before the application starts. */
    public static void validate() {
        require(L1 >= 0.0 && L1 <= 100.0, "L1 must be in [0,100]");
        require(L2 >= 0.0 && L2 <= 100.0, "L2 must be in [0,100]");
        require(L1 < L2, "L1 must be lower than L2");
        require(T1_MS > 0, "T1 must be > 0");
        require(T2_MS > 0, "T2 must be > 0");
        require(HISTORY_SIZE > 0, "History size must be > 0");
        require(CONTROL_TICK_MS > 0, "Control tick must be > 0");
        require(HTTP_PORT > 0 && HTTP_PORT <= 65535, "HTTP port must be valid");
        require(!HTTP_HOST.isBlank(), "HTTP host cannot be blank");
        require(!DBS_DIRECTORY.isBlank(), "DBS directory cannot be blank");

        if (SERIAL_ENABLED) {
            require(!SERIAL_PORT.isBlank(), "Serial port cannot be blank");
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

    private static String stringSetting(
        final String property,
        final String environment,
        final String defaultValue
    ) {
        final String propertyValue = System.getProperty(property);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        final String environmentValue = System.getenv(environment);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }

        return defaultValue;
    }

    private static int intSetting(
        final String property,
        final String environment,
        final int defaultValue
    ) {
        return Integer.parseInt(stringSetting(property, environment, Integer.toString(defaultValue)));
    }

    private static long longSetting(
        final String property,
        final String environment,
        final long defaultValue
    ) {
        return Long.parseLong(stringSetting(property, environment, Long.toString(defaultValue)));
    }

    private static double doubleSetting(
        final String property,
        final String environment,
        final double defaultValue
    ) {
        return Double.parseDouble(stringSetting(property, environment, Double.toString(defaultValue)));
    }

    private static boolean booleanSetting(
        final String property,
        final String environment,
        final boolean defaultValue
    ) {
        return Boolean.parseBoolean(
            stringSetting(property, environment, Boolean.toString(defaultValue))
        );
    }
}
