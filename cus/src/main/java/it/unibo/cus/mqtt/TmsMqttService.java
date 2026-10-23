package it.unibo.cus.mqtt;

import it.unibo.cus.Config;
import it.unibo.cus.core.SystemController;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** MQTT subscriber that receives TMS water-level samples through Eclipse Paho. */
public final class TmsMqttService implements AutoCloseable {

    private final SystemController controller;
    private final TmsMessageParser parser;

    private volatile boolean running;
    private boolean closed;
    private Thread connectionThread;
    private MqttClient client;

    public TmsMqttService(
        final SystemController controller,
        final TmsMessageParser parser
    ) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public synchronized void start() {
        if (closed) {
            throw new IllegalStateException("MQTT service has already been closed");
        }
        if (running) {
            return;
        }

        running = true;
        connectionThread = new Thread(this::connectionLoop, "cus-tms-mqtt");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @Override
    public void close() {
        final Thread threadToStop;
        final MqttClient clientToClose;

        synchronized (this) {
            if (closed) {
                return;
            }

            closed = true;
            running = false;
            threadToStop = connectionThread;
            connectionThread = null;
            clientToClose = client;
            client = null;
        }

        if (threadToStop != null) {
            threadToStop.interrupt();
        }
        closeMqttClient(clientToClose);
    }

    /** Retries the initial connection, then leaves reconnections to Paho. */
    private void connectionLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                final MqttClient connectedClient = connectClient();

                synchronized (this) {
                    if (running) {
                        client = connectedClient;
                        return;
                    }
                }

                // Shutdown was requested while connectClient() was running.
                closeMqttClient(connectedClient);
                return;
            } catch (final MqttException exception) {
                if (!running || Thread.currentThread().isInterrupted()) {
                    return;
                }

                System.err.println("========== MQTT ERROR ==========");
                System.err.println("Broker: " + Config.MQTT_BROKER_URI);
                System.err.println("Error code: " + exception.getReasonCode());
                System.err.println("Message: " + exception.getMessage());
                System.err.println("Cause: " + exception.getCause());
                exception.printStackTrace();
                System.err.println("================================");

                sleepBeforeRetry();
            }
        }
    }

    /** Returns a connected and subscribed client; cleans it up on failure. */
    private MqttClient connectClient() throws MqttException {
        final String clientId = "assignment03-cus-" + System.currentTimeMillis();
        final MqttClient newClient = new MqttClient(Config.MQTT_BROKER_URI, clientId);

        try {
            newClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(
                    final boolean reconnect,
                    final String serverURI
                ) {
                    if (!reconnect || !running) {
                        return;
                    }

                    try {
                        newClient.subscribe(Config.MQTT_TOPIC, Config.MQTT_QOS);
                        System.out.println(
                            "[MQTT] Reconnected to " + serverURI
                                + ", subscribed to " + Config.MQTT_TOPIC
                        );
                    } catch (final MqttException exception) {
                        System.err.println(
                            "[MQTT] Subscribe failed: " + exception.getMessage()
                        );
                    }
                }

                @Override
                public void connectionLost(final Throwable cause) {
                    if (running) {
                        final String message = cause == null
                            ? "unknown cause"
                            : cause.getMessage();
                        System.err.println("[MQTT] Connection lost: " + message);
                    }
                }

                @Override
                public void messageArrived(
                    final String topic,
                    final MqttMessage message
                ) {
                    if (running) {
                        processMessage(message);
                    }
                }

                @Override
                public void deliveryComplete(final IMqttDeliveryToken token) {
                    // Not used by a subscriber.
                }
            });

            final MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            newClient.connect(options);

            // Skip subscribing if the service was closed during connect().
            if (running) {
                newClient.subscribe(Config.MQTT_TOPIC, Config.MQTT_QOS);
                System.out.println(
                    "[MQTT] Connected to " + Config.MQTT_BROKER_URI
                        + ", subscribed to " + Config.MQTT_TOPIC
                );
            }

            return newClient;
        } catch (final MqttException | RuntimeException exception) {
            closeMqttClient(newClient);
            throw exception;
        }
    }

    private void processMessage(final MqttMessage message) {
        final String payload = new String(
            message.getPayload(),
            StandardCharsets.UTF_8
        );

        final Double value;
        try {
            value = parser.parse(payload);
        } catch (final IllegalArgumentException exception) {
            // Malformed messages are discarded and do not reset T2.
            System.err.println(
                "[MQTT] Invalid TMS payload: " + exception.getMessage()
            );
            return;
        }

        if (!running) {
            return;
        }

        controller.onMeasurement(value, System.currentTimeMillis());
        System.out.println(
            "[MQTT] TMS sample: " + (value == null ? "null" : value)
        );
    }

    /** Attempts cleanup even when disconnect() fails. */
    private void closeMqttClient(final MqttClient mqttClient) {
        if (mqttClient == null) {
            return;
        }

        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (final MqttException exception) {
            System.err.println(
                "[MQTT] Disconnect error: " + exception.getMessage()
            );
        }

        try {
            mqttClient.close();
        } catch (final MqttException exception) {
            System.err.println(
                "[MQTT] Close error: " + exception.getMessage()
            );
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(Config.MQTT_RETRY_DELAY_MS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
