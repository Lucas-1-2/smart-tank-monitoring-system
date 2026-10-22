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
    private Thread connectionThread;
    private volatile MqttClient client;

    public TmsMqttService(
        final SystemController controller,
        final TmsMessageParser parser
    ) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        connectionThread = new Thread(this::connectionLoop, "cus-tms-mqtt");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @Override
    public synchronized void close() {
        running = false;

        if (connectionThread != null) {
            connectionThread.interrupt();
        }

        final MqttClient localClient = client;
        if (localClient != null) {
            try {
                if (localClient.isConnected()) {
                    localClient.disconnect();
                }
                localClient.close();
            } catch (final MqttException exception) {
                System.err.println("[MQTT] Close error: " + exception.getMessage());
            }
        }
    }

    private void connectionLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                if (client == null) {
                    connectClient();
                }

                Thread.sleep(Config.MQTT_RETRY_DELAY_MS);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (final MqttException exception) {
                System.err.println("[MQTT] Connection failed: " + exception.getMessage());
                closeClientAfterFailure();
                sleepBeforeRetry();
            }
        }
    }

    private void connectClient() throws MqttException {
        final String clientId = "assignment03-cus-" + System.currentTimeMillis();
        final MqttClient newClient = new MqttClient(Config.MQTT_BROKER_URI, clientId);
        client = newClient;

        newClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(final boolean reconnect, final String serverURI) {
                if (!reconnect) {
                    return;
                }

                try {
                    newClient.subscribe(Config.MQTT_TOPIC, Config.MQTT_QOS);
                    System.out.println(
                        "[MQTT] Reconnected to " + serverURI
                            + ", subscribed to " + Config.MQTT_TOPIC
                    );
                } catch (final MqttException exception) {
                    System.err.println("[MQTT] Subscribe failed: " + exception.getMessage());
                }
            }

            @Override
            public void connectionLost(final Throwable cause) {
                final String message = cause == null ? "unknown cause" : cause.getMessage();
                System.err.println("[MQTT] Connection lost: " + message);
            }

            @Override
            public void messageArrived(final String topic, final MqttMessage message) {
                processMessage(message);
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

        newClient.subscribe(Config.MQTT_TOPIC, Config.MQTT_QOS);
        System.out.println(
            "[MQTT] Connected to " + Config.MQTT_BROKER_URI
                + ", subscribed to " + Config.MQTT_TOPIC
        );
    }

    private void processMessage(final MqttMessage message) {
        final String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        final Double value;

        try {
            value = parser.parse(payload);
        } catch (final IllegalArgumentException exception) {
            System.err.println("[MQTT] Invalid TMS payload: " + exception.getMessage());
            return;
        }

        controller.onMeasurement(value, System.currentTimeMillis());
        System.out.println("[MQTT] TMS sample: " + (value == null ? "null" : value));
    }

    private void closeClientAfterFailure() {
        final MqttClient localClient = client;
        client = null;

        if (localClient != null) {
            try {
                localClient.close();
            } catch (final MqttException ignored) {
                // Best-effort cleanup before retry.
            }
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
