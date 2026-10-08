package it.unibo.cus.core;

/**
 * Finite-state implementation of the AUTOMATIC valve policy.
 *
 * LOW:          x <= L1 -> valve 0%
 * MID_WAITING:  L1 < x < L2 for less than T1 -> keep previous opening
 * MID_ACTIVE:   L1 < x < L2 for at least T1 -> valve 50%
 * HIGH:         x >= L2 -> valve 100%
 */
public final class AutomaticPolicy {

    public enum State {
        UNKNOWN,
        LOW,
        MID_WAITING,
        MID_ACTIVE,
        HIGH
    }

    private final double l1;
    private final double l2;
    private final long t1Ms;

    private State state;
    private long midSince;

    public AutomaticPolicy(final double l1, final double l2, final long t1Ms) {
        if (
            !Double.isFinite(l1) ||
            !Double.isFinite(l2) || 
            l1 < 0.0 || 
            l2 > 100.0 || 
            l1 >= l2
        ) {
            throw new IllegalArgumentException("Invalid automatic policy thresholds");
        }
        if (t1Ms <= 0) {
            throw new IllegalArgumentException("T1 must be > 0");
        }

        this.l1 = l1;
        this.l2 = l2;
        this.t1Ms = t1Ms;
        reset();
    }

    /** Resets temporal knowledge without changing the valve directly. */
    public void reset() {
        state = State.UNKNOWN;
        midSince = -1L;
    }

    /**
     * Processes a valid water-level measurement.
     * @param level valid water-level percentage
     * @param currentValve current valve opening
     * @param now current time in milliseconds
     * @return valve opening required by the policy
     */
    public int onMeasurement(
        final double level,
        final int currentValve,
        final long now
    ) {
        if (!Double.isFinite(level) || level < 0.0 || level > 100.0) {
            throw new IllegalArgumentException("Water level must be in [0,100]");
        }
        validateValve(currentValve);

        if (level <= l1) {
            state = State.LOW;
            midSince = -1L;
            return 0;
        }

        if (level >= l2) {
            state = State.HIGH;
            midSince = -1L;
            return 100;
        }

        if (state != State.MID_WAITING && state != State.MID_ACTIVE) {
            state = State.MID_WAITING;
            midSince = now;
            return currentValve;
        }

        if (state == State.MID_WAITING && hasT1Elapsed(now)) {
            state = State.MID_ACTIVE;
            return 50;
        }

        return state == State.MID_ACTIVE ? 50 : currentValve;
    }

    /**
     * Handles time progression while no new valid measurement is needed.
     * Only MID_WAITING can produce a transition on time alone.
     */
    public int onTick(final int currentValve, final long now) {
        validateValve(currentValve);

        if (state == State.MID_WAITING && hasT1Elapsed(now)) {
            state = State.MID_ACTIVE;
            return 50;
        }

        return currentValve;
    }

    public State getState() {
        return state;
    }

    private boolean hasT1Elapsed(final long now) {
        return midSince >= 0L && now - midSince >= t1Ms;
    }

    private void validateValve(final int valve) {
        if (valve < 0 || valve > 100) {
            throw new IllegalArgumentException("Valve opening must be in [0,100]");
        }
    }
}
