package it.unibo.cus.mqtt;

/**
 * Converts one TMS MQTT payload into a water-level percentage.
 * A null return value represents an invalid/unavailable sensor reading.
 */
@FunctionalInterface
public interface TmsMessageParser {
    Double parse(String payload);
}
