package it.unibo.cus.core.api;

import it.unibo.cus.model.Measurement;
import java.util.List;

/**
 * Stores the most recent measurements received from the TMS.
 *
 * The history keeps a limited number of samples in memory.
 * A sample can have a null value when the sensor reports NO_ECHO.
 * The history is used by the DBS to show recent water-level values.
 */
public interface MeasurementHistory {

    /**
     * Adds a new measurement to the history.
     * The oldest sample is removed when the history is full.
     *
     * @param measurement the measurement to store
     */
    void add(Measurement measurement);

    /**
     * Gets a copy of the current measurement history.
     * The result is ordered from oldest to newest and cannot be modified.
     *
     * @return the stored measurements
     */
    List<Measurement> snapshot();
}
