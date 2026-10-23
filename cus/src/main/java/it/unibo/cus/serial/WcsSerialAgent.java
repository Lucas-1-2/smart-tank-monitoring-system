package it.unibo.cus.serial;

import it.unibo.cus.core.CommandResult;
import it.unibo.cus.core.SystemController;
import it.unibo.cus.model.OperatingMode;
import it.unibo.cus.model.SystemSnapshot;

import java.util.Locale;
import java.util.Objects;

/**
 * Bridges the WCS line protocol and the thread-safe SystemController.
 * Receiving and sending are kept on separate threads.
 */
public final class WcsSerialAgent implements AutoCloseable {

    private final SerialCommChannel channel;
    private final SystemController controller;

    private volatile boolean running;
    private boolean closed;
    private Thread receiverThread;
    private Thread writerThread;

    public WcsSerialAgent(
        final SerialCommChannel channel,
        final SystemController controller
    ) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public synchronized void start() {
        if (closed) {
            throw new IllegalStateException(
                "Serial agent has already been closed"
            );
        }

        if (running) {
            return;
        }

        running = true;

        receiverThread = new Thread(this::receiveLoop, "cus-wcs-serial-receiver");
        receiverThread.setDaemon(true);

        writerThread = new Thread(this::writerLoop, "cus-wcs-serial-writer");
        writerThread.setDaemon(true);

        receiverThread.start();
        writerThread.start();

        /* Initial synchronization, also repeated when WCS sends READY. */
        controller.requestWcsStateUpdate();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        closed = true;
        running = false;

        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        if (writerThread != null) {
            writerThread.interrupt();
        }

        channel.close();
    }

    private void receiveLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                processMessage(channel.receiveMsg());
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (final RuntimeException exception) {
                System.err.println("[SERIAL] " + exception.getMessage());
            }
        }
    }

    private void writerLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                final SystemSnapshot snapshot = controller.takeWcsStateUpdate();
                channel.sendMsg(formatState(snapshot));
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (final RuntimeException exception) {
                System.err.println("[SERIAL] Send error: " + exception.getMessage());
            }
        }
    }

    private void processMessage(final String message) {
        if (message.startsWith("LOG,")) {
            System.out.println("[WCS] " + message.substring(4));
            return;
        }

        if (message.equals("READY")) {
            controller.requestWcsStateUpdate();
            return;
        }

        if (!message.startsWith("CMD,")) {
            System.err.println("[SERIAL] Ignored message: " + message);
            return;
        }

        final String[] parts = message.split(",", -1);
        if (parts.length < 3) {
            System.err.println("[SERIAL] Invalid command: " + message);
            controller.requestWcsStateUpdate();
            return;
        }

        final String type = parts[1].trim().toUpperCase(Locale.ROOT);

        switch (type) {
            case "MODE" -> processModeCommand(parts, message);
            case "VALVE" -> processValveCommand(parts, message);
            default -> {
                System.err.println("[SERIAL] Unknown command: " + message);
                controller.requestWcsStateUpdate();
            }
        }
    }

    private void processModeCommand(final String[] parts, final String completeMessage) {
        if (parts.length != 3) {
            System.err.println("[SERIAL] Invalid MODE command: " + completeMessage);
            controller.requestWcsStateUpdate();
            return;
        }

        final OperatingMode requestedMode;
        try {
            requestedMode = OperatingMode.valueOf(
                parts[2].trim().toUpperCase(Locale.ROOT)
            );
        } catch (final IllegalArgumentException exception) {
            System.err.println("[SERIAL] Invalid mode: " + completeMessage);
            controller.requestWcsStateUpdate();
            return;
        }

        final CommandResult result = controller.requestMode(requestedMode);
        ensureResponseWhenNoStateChange(result);
    }

    private void processValveCommand(final String[] parts, final String completeMessage) {
        if (parts.length != 3) {
            System.err.println("[SERIAL] Invalid VALVE command: " + completeMessage);
            controller.requestWcsStateUpdate();
            return;
        }

        final int opening;
        try {
            opening = Integer.parseInt(parts[2].trim());
        } catch (final NumberFormatException exception) {
            System.err.println("[SERIAL] Invalid valve value: " + completeMessage);
            controller.requestWcsStateUpdate();
            return;
        }

        final CommandResult result;
        try {
            result = controller.requestValveOpening(opening);
        } catch (final IllegalArgumentException exception) {
            System.err.println("[SERIAL] " + exception.getMessage());
            controller.requestWcsStateUpdate();
            return;
        }

        ensureResponseWhenNoStateChange(result);
    }

    /**
     * State-changing accepted commands are already queued by the controller.
     * Rejected/no-op commands still receive the authoritative current state.
     */
    private void ensureResponseWhenNoStateChange(final CommandResult result) {
        if (!result.changed()) {
            controller.requestWcsStateUpdate();
        }
    }

    private String formatState(final SystemSnapshot snapshot) {
        return "STATE,"
            + snapshot.state().name()
            + ","
            + snapshot.valveOpening();
    }
}
