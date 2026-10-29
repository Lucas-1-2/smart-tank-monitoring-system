package it.unibo.cus.core.api;

import it.unibo.cus.core.CommandResult;
import it.unibo.cus.model.Measurement;
import it.unibo.cus.model.OperatingMode;
import it.unibo.cus.model.SystemSnapshot;
import java.util.List;

/**
 * Manages the shared state of the Control Unit Subsystem (CUS).
 *
 * This is the main point of access for MQTT measurements, HTTP requests,
 * WCS commands and timer events. The controller starts in AUTOMATIC mode.
 * It uses an {@link AutomaticPolicy} to calculate the valve opening and a
 * {@link MeasurementHistory} to store the last TMS samples.
 *
 * If no valid TMS sample arrives during T2, the public state becomes
 * UNCONNECTED and the valve opening becomes 5%. A new valid sample restores
 * AUTOMATIC mode. The controller also sends update signals to the WCS agent.
 *
 * Implementations must allow calls from different threads. Serial, HTTP
 * and MQTT input/output is handled by the other services.
 */
public interface SystemController {

    /**
     * Processes a TMS measurement using the given time.
     * A null value is stored as an unavailable reading. It does not reset T2.
     * In AUTOMATIC mode, a null value also resets the T1 waiting period.
     *
     * @param value the water level from 0 to 100, or null for NO_ECHO
     * @param now the measurement time in milliseconds
     */
    void onMeasurement(Double value, long now);

    /**
     * Processes a TMS measurement using the current system time.
     *
     * @param value the water level from 0 to 100, or null for NO_ECHO
     */
    void onMeasurement(Double value);

    /**
     * Checks the T1 and T2 timers using the given time.
     * T1 can change the automatic valve opening. T2 can set UNCONNECTED.
     *
     * @param now the current time in milliseconds
     */
    void tick(long now);

    /**
     * Checks the T1 and T2 timers using the current system time.
     */
    void tick();

    /**
     * Requests a change between AUTOMATIC and MANUAL mode.
     * The request is rejected while the system is UNCONNECTED.
     * Entering AUTOMATIC restarts the automatic policy.
     *
     * @param requestedMode the mode to activate
     * @return the result and the latest system state
     * @throws NullPointerException if requestedMode is null
     */
    CommandResult requestMode(OperatingMode requestedMode);

    /**
     * Requests a new valve opening in MANUAL mode.
     * The request is rejected outside MANUAL mode or while UNCONNECTED.
     *
     * @param requestedOpening the requested opening from 0 to 100
     * @return the result and the latest system state
     * @throws IllegalArgumentException if the opening is out of range
     */
    CommandResult requestValveOpening(int requestedOpening);

    /**
     * Gets the current public state, valve opening and water level.
     * The returned object does not change when the controller is updated.
     *
     * @return a snapshot of the current state
     */
    SystemSnapshot getSnapshot();

    /**
     * Gets the stored TMS measurements, ordered from oldest to newest.
     *
     * @return an independent, unmodifiable list of measurements
     */
    List<Measurement> getMeasurementsSnapshot();

    /**
     * Requests a WCS state refresh.
     * Repeated requests can be grouped into one pending signal.
     * The WCS agent reads the latest state when it handles the signal.
     */
    void requestWcsStateUpdate();

    /**
     * Waits for a WCS update request and then gets the latest state.
     * This method can block until an update is available.
     *
     * @return the latest state to send to the WCS
     * @throws InterruptedException if the waiting thread is interrupted
     */
    SystemSnapshot takeWcsStateUpdate() throws InterruptedException;
}
