package it.unibo.cus.serial.api;

/**
 * Connects the Water Control Subsystem (WCS) to the CUS controller.
 *
 * The agent receives READY and CMD messages from the WCS and forwards
 * requests to the controller. It sends STATE messages with the current
 * mode and valve opening. LOG messages are printed for debugging.
 *
 * Receiving and sending use separate threads. State updates are read
 * from the controller, which is the source of the system state.
 */
public interface WcsSerialAgent extends AutoCloseable {

    /**
     * Starts the serial reader and writer threads.
     * It also requests the first WCS state update.
     * Calling start again while running has no effect.
     *
     * @throws IllegalStateException if the agent has been closed
     */
    void start();

    /**
     * Stops the threads and closes the serial channel.
     * A closed agent cannot be started again.
     */
    @Override
    void close();
}
