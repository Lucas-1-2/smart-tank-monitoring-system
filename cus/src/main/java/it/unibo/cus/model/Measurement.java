package it.unibo.cus.model;

/**
 * One TMS measurement retained for the dashboard graph.
 * @param timestamp measurement reception time in milliseconds since epoch
 * @param value water level percentage, null for an invalid/unavailable sample
 */
public record Measurement(long timestamp, Double value) {
    public Measurement {
        if (timestamp < 0) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }

        if (value != null
            && (!Double.isFinite(value) || value < 0.0 || value > 100.0)) {
            throw new IllegalArgumentException("Measurement must be null or in [0,100]");
        }
    }
}
