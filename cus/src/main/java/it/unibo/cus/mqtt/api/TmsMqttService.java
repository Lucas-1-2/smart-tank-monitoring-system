package it.unibo.cus.mqtt.api;

/**
 * Receives tank measurements from the TMS through MQTT.
 *
 * Messages are parsed by a {@link TmsMessageParser} and passed to the
 * system controller. Invalid messages are ignored. A NO_ECHO message is
 * passed as a null measurement, so it does not restart the T2 timer.
 * The service retries the first connection and handles later reconnects.
 */
public interface TmsMqttService extends AutoCloseable {

    /**
     * Starts the MQTT connection in a separate thread.
     * Calling start again while running has no effect.
     *
     * @throws IllegalStateException if the service has been closed
     */
    void start();

    /**
     * Stops the service and closes the MQTT connection.
     * A closed service cannot be started again.
     */
    @Override
    void close();
}
