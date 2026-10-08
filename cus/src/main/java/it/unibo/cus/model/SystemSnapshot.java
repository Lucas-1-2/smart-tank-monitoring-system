package it.unibo.cus.model;

import java.util.Objects;

/**
 * Immutable snapshot of the global state exposed outside the controller.
 * @param state public operational state
 * @param valveOpening valve opening percentage
 * @param waterLevel current water level, null when unavailable
 */
public record SystemSnapshot(
    PublicState state,
    int valveOpening,
    Double waterLevel
) {
    public SystemSnapshot {
        Objects.requireNonNull(state, "state");

        if (valveOpening < 0 || valveOpening > 100) {
            throw new IllegalArgumentException("Valve opening must be in [0,100]");
        }

        if (waterLevel != null
            && (!Double.isFinite(waterLevel) || waterLevel < 0.0 || waterLevel > 100.0)) {
            throw new IllegalArgumentException("Water level must be null or in [0,100]");
        }
    }
}
