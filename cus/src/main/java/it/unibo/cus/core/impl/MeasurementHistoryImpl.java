package it.unibo.cus.core.impl;

import it.unibo.cus.core.api.MeasurementHistory;

import it.unibo.cus.model.Measurement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Fixed-size in-memory history of the latest TMS measurements. */
public final class MeasurementHistoryImpl implements MeasurementHistory {

    private final int maxSize;
    private final Deque<Measurement> measurements;

    /**
     * Creates a history with the given maximum size.
     *
     * @param maxSize the maximum number of measurements to keep
     * @throws IllegalArgumentException if maxSize is not positive
     */
    public MeasurementHistoryImpl(final int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }

        this.maxSize = maxSize;
        this.measurements = new ArrayDeque<>(maxSize);
    }

    @Override
    public void add(final Measurement measurement) {
        measurements.addLast(measurement);

        while (measurements.size() > maxSize) {
            measurements.removeFirst();
        }
    }

    @Override
    public List<Measurement> snapshot() {
        return List.copyOf(measurements);
    }
}
