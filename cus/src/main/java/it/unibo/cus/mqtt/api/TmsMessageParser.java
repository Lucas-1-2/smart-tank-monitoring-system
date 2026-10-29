package it.unibo.cus.mqtt.api;

/**
 * Reads a TMS MQTT message and gets the water-level percentage.
 *
 * A message with status OK contains a percentage from 0 to 100.
 * A message with status NO_ECHO represents a missing sensor reading.
 * The parser only checks the payload. The controller manages T1 and T2.
 */
@FunctionalInterface
public interface TmsMessageParser {

    /**
     * Parses one JSON payload from the TMS.
     *
     * @param payload the MQTT message text
     * @return the water level from 0 to 100, or null for NO_ECHO
     * @throws IllegalArgumentException if the payload is invalid
     */
    Double parse(String payload);
}
