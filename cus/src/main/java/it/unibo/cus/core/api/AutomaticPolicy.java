package it.unibo.cus.core.api;

/**
 * Defines how the valve opening is selected in AUTOMATIC mode.
 *
 * The policy uses the current water level and two thresholds, L1 and L2.
 * It also uses the T1 waiting time for values between the thresholds.
 * This component only calculates the valve opening. It does not control
 * the WCS or change the global system mode.
 *
 * The policy works as follows:
 * - At or below L1, the valve opening is 0%.
 * - At or above L2, the valve opening is 100%.
 * - Between L1 and L2, the previous opening is kept during T1.
 * - After T1 in the middle range, the opening is 50%.
 */
public interface AutomaticPolicy {

    /** States used by the automatic policy. */
    enum State {
        /** No valid measurement has been processed since the last reset. */
        UNKNOWN,
        /** The level is at or below L1. */
        LOW,
        /** The level is between L1 and L2, and T1 has not ended. */
        MID_WAITING,
        /** The level is between L1 and L2, and T1 has ended. */
        MID_ACTIVE,
        /** The level is at or above L2. */
        HIGH
    }

    /**
     * Clears the previous state and the T1 timer.
     * This method does not move the valve.
     */
    void reset();

    /**
     * Gets the current state of the automatic policy.
     *
     * @return the current policy state
     */
    State getState();

    /**
     * Updates the policy with a new valid water level.
     *
     * @param level the water level, from 0 to 100
     * @param currentValve the current valve opening, from 0 to 100
     * @param now the current time in milliseconds
     * @return the valve opening requested by the policy
     * @throws IllegalArgumentException if the level or valve is out of range
     */
    int onMeasurement(double level, int currentValve, long now);

    /**
     * Checks if T1 has ended, even when no new measurement arrives.
     * Only a waiting middle-range state can change on a tick.
     *
     * @param currentValve the current valve opening, from 0 to 100
     * @param now the current time in milliseconds
     * @return the requested opening, or the current opening if nothing changes
     * @throws IllegalArgumentException if the valve is out of range
     */
    int onTick(int currentValve, long now);
}
