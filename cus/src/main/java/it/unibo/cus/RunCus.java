package it.unibo.cus;

import it.unibo.cus.core.api.SystemController;
import it.unibo.cus.core.impl.SystemControllerImpl;
import it.unibo.cus.http.api.HttpService;
import it.unibo.cus.http.impl.HttpServiceImpl;
import it.unibo.cus.mqtt.api.TmsMqttService;
import it.unibo.cus.mqtt.impl.TmsMessageParserImpl;
import it.unibo.cus.mqtt.impl.TmsMqttServiceImpl;
import it.unibo.cus.serial.api.SerialCommChannel;
import it.unibo.cus.serial.api.WcsSerialAgent;
import it.unibo.cus.serial.impl.SerialCommChannelImpl;
import it.unibo.cus.serial.SerialPortSelector;
import it.unibo.cus.serial.impl.WcsSerialAgentImpl;

import io.vertx.core.Vertx;

import java.util.concurrent.atomic.AtomicReference;

/** Application entry point for the Control Unit Subsystem. */
public final class RunCus {

    private RunCus() {
    }

    public static void main(final String[] args) {
        try {
            Config.validate();
        } catch (final IllegalStateException exception) {
            System.err.println("[CUS] Configuration error: " + exception.getMessage());
            return;
        }

        printConfiguration();

        final SystemController controller = new SystemControllerImpl();
        final Vertx vertx = Vertx.vertx();

        final AtomicReference<WcsSerialAgent> serialAgentRef = new AtomicReference<>();
        final AtomicReference<TmsMqttService> mqttServiceRef = new AtomicReference<>();

        registerShutdownHook(vertx, serialAgentRef, mqttServiceRef);
        startHttpService(vertx, controller);

        vertx.setPeriodic(
            Config.CONTROL_TICK_MS,
            ignored -> controller.tick(System.currentTimeMillis())
        );

        if (Config.MQTT_ENABLED) {
            final TmsMqttService mqttService = new TmsMqttServiceImpl(
                controller,
                new TmsMessageParserImpl()
            );
            mqttServiceRef.set(mqttService);
            mqttService.start();
        }

        if (Config.SERIAL_ENABLED) {
            startSerialService(controller, serialAgentRef);
        }
    }

    private static void startHttpService(
        final Vertx vertx,
        final SystemController controller
    ) {
        final HttpService httpService = new HttpServiceImpl(controller);
        vertx.deployVerticle(httpService, result -> {
            if (result.failed()) {
                System.err.println(
                    "[CUS] Cannot start HTTP service: " + result.cause().getMessage()
                );
            }
        });
    }

    private static void startSerialService(
        final SystemController controller,
        final AtomicReference<WcsSerialAgent> serialAgentRef
    ) {
        try {
            final String portName = Config.SERIAL_PORT != null
                ? Config.SERIAL_PORT
                : SerialPortSelector.selectPort();

            final SerialCommChannel channel = new SerialCommChannelImpl(
                portName,
                Config.SERIAL_BAUD_RATE
            );

            final WcsSerialAgent serialAgent = new WcsSerialAgentImpl(channel, controller);
            serialAgentRef.set(serialAgent);

            try {
                serialAgent.start();
            } catch (final RuntimeException exception) {
                serialAgent.close();
                serialAgentRef.set(null);
                throw exception;
            }

            System.out.println(
                "[SERIAL] WCS connected on " + portName
                    + " @ " + Config.SERIAL_BAUD_RATE
            );
        } catch (final Exception exception) {
            final Throwable cause = exception.getCause() != null
                ? exception.getCause()
                : exception;
            final String message = cause.getMessage() != null
                ? cause.getMessage()
                : cause.getClass().getSimpleName();

            System.err.println("[SERIAL] WCS serial service not started: " + message);
            System.err.println(
                "[SERIAL] Set cus.serial.port in cus.properties to bypass selection, "
                    + "or set cus.serial.enabled=false to disable serial communication."
            );
        }
    }

    private static void registerShutdownHook(
        final Vertx vertx,
        final AtomicReference<WcsSerialAgent> serialAgentRef,
        final AtomicReference<TmsMqttService> mqttServiceRef
    ) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[CUS] Shutting down...");

            final WcsSerialAgent serialAgent = serialAgentRef.get();
            if (serialAgent != null) {
                serialAgent.close();
            }

            final TmsMqttService mqttService = mqttServiceRef.get();
            if (mqttService != null) {
                mqttService.close();
            }

            vertx.close();
        }, "cus-shutdown"));
    }

    private static void printConfiguration() {
        System.out.println("[CUS] Starting Assignment 03 Control Unit Subsystem");
        System.out.println("[CUS] L1=" + Config.L1 + "% L2=" + Config.L2 + "%");
        System.out.println("[CUS] T1=" + Config.T1_MS + " ms T2=" + Config.T2_MS + " ms");
        System.out.println("[CUS] History size=" + Config.HISTORY_SIZE);
        System.out.println("[CUS] HTTP=" + Config.HTTP_HOST + ":" + Config.HTTP_PORT);
        System.out.println("[CUS] DBS directory=" + Config.DBS_DIRECTORY);

        if (Config.MQTT_ENABLED) {
            System.out.println("[CUS] MQTT broker=" + Config.MQTT_BROKER_URI);
            System.out.println("[CUS] MQTT topic=" + Config.MQTT_TOPIC);
        } else {
            System.out.println("[CUS] MQTT disabled");
        }

        if (Config.SERIAL_ENABLED) {
            final String portDescription = Config.SERIAL_PORT == null
                ? "interactive selection"
                : Config.SERIAL_PORT;
            System.out.println("[CUS] Serial port=" + portDescription);
            System.out.println("[CUS] Serial baud=" + Config.SERIAL_BAUD_RATE);
        } else {
            System.out.println("[CUS] Serial disabled");
        }
    }
}
