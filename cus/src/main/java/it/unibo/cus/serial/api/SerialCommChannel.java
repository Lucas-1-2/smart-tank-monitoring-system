package it.unibo.cus.serial.api;

/**
 * Sends and receives text messages over a serial connection.
 *
 * Each message is one line of ASCII text. A newline ends the message.
 * Incoming lines are placed in a queue, so they can be read in order.
 * This interface only handles communication, not WCS commands.
 */
public interface SerialCommChannel extends AutoCloseable {

    /**
     * Sends a message followed by a newline character.
     *
     * @param message the message to send
     * @throws IllegalStateException if the full message cannot be sent
     */
    void sendMsg(String message);

    /**
     * Waits until the next complete message is available.
     * Empty lines are not returned.
     *
     * @return the next received message without the newline
     * @throws InterruptedException if the waiting thread is interrupted
     */
    String receiveMsg() throws InterruptedException;

    /**
     * Closes the serial connection and stops receiving messages.
     */
    @Override
    void close();
}
