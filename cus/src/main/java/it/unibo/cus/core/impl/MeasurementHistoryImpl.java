package it.unibo.cus.core.impl;

import it.unibo.cus.model.Measurement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Fixed-size in-memory history of the latest TMS measurements. */
public final class MeasurementHistoryImpl {

    private final int maxSize;
    private final Deque<Measurement> measurements;

    public MeasurementHistoryImpl(final int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }

        this.maxSize = maxSize;
        this.measurements = new ArrayDeque<>(maxSize);
    }

    public void add(final Measurement measurement) {
        measurements.addLast(measurement);

        while (measurements.size() > maxSize) {
            measurements.removeFirst();
        }
    }

    public List<Measurement> snapshot() {
        return List.copyOf(measurements);
    }
}
