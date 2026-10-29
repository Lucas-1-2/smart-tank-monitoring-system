package it.unibo.cus.serial.impl;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import it.unibo.cus.serial.api.SerialCommChannel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Line-based serial communication channel.
 * Adapted from the Smart Hangar DRU implementation.
 */
public final class SerialCommChannelImpl implements SerialCommChannel {

    private static final int MESSAGE_QUEUE_CAPACITY = 100;

    private final SerialPort serialPort;
    private final BlockingQueue<String> queue;
    private final StringBuilder currentMsg;

    /**
     * Opens a serial port and starts reading incoming lines.
     *
     * @param port the serial port name
     * @param rate the serial baud rate
     * @throws Exception if the serial port cannot be opened
     */
    public SerialCommChannelImpl(final String port, final int rate) throws Exception {
        queue = new ArrayBlockingQueue<>(MESSAGE_QUEUE_CAPACITY);
        currentMsg = new StringBuilder();

        serialPort = SerialPort.getCommPort(port);
        serialPort.setBaudRate(rate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);

        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Cannot open serial port " + port);
        }

        final boolean listenerRegistered =
            serialPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(final SerialPortEvent event) {
                    if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        return;
                    }

                    final int availableBytes = serialPort.bytesAvailable();
                    if (availableBytes <= 0) {
                        return;
                    }

                    final byte[] newData = new byte[availableBytes];
                    final int numRead = serialPort.readBytes(newData, newData.length);

                    if (numRead <= 0) {
                        return;
                    }

                    final String chunk = new String(
                        newData,
                        0,
                        numRead,
                        StandardCharsets.US_ASCII
                    ).replace("\r", "");

                    currentMsg.append(chunk);
                    processMessages();
                }
            });

        if (!listenerRegistered) {
            serialPort.closePort();
            throw new IllegalStateException(
                "Cannot register serial listener on port " + port
            );
        }
    }

    @Override
    public void sendMsg(final String message) {
        final byte[] bytes = (message + "\n").getBytes(StandardCharsets.US_ASCII);

        synchronized (serialPort) {
            final int written = serialPort.writeBytes(bytes, bytes.length);

            if (written != bytes.length) {
                throw new IllegalStateException(
                    "Serial write failed: " + written
                        + "/" + bytes.length + " bytes written"
                );
            }
        }
    }

    @Override
    public String receiveMsg() throws InterruptedException {
        return queue.take();
    }

    @Override
    public void close() {
        serialPort.removeDataListener();
        serialPort.closePort();
    }

    private void processMessages() {
        int newlineIndex = currentMsg.indexOf("\n");

        while (newlineIndex >= 0) {
            final String message = currentMsg
                .substring(0, newlineIndex)
                .trim();

            currentMsg.delete(0, newlineIndex + 1);

            if (!message.isEmpty()) {
                try {
                    queue.put(message);
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            newlineIndex = currentMsg.indexOf("\n");
        }
    }
}
