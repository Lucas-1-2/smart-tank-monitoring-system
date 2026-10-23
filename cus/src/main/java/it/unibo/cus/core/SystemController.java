package it.unibo.cus.core;

import it.unibo.cus.Config;
import it.unibo.cus.model.Measurement;
import it.unibo.cus.model.OperatingMode;
import it.unibo.cus.model.PublicState;
import it.unibo.cus.model.SystemSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Thread-safe owner of the global CUS state.
 *
 * All MQTT, HTTP, serial and timer events enter through this class.
 * No I/O is performed while the internal lock is held.
 */
public final class SystemController {

    private final Object lock;
    private final AutomaticPolicy automaticPolicy;
    private final MeasurementHistory measurementHistory;
    private final BlockingQueue<Boolean> wcsUpdateSignals;

    private OperatingMode mode;
    private boolean tmsAvailable;
    private Double waterLevel;
    private int valveOpening;
    private long lastValidMeasurementTime;

    public SystemController() {
        this(System.currentTimeMillis());
    }

    SystemController(final long bootTime) {
        lock = new Object();
        automaticPolicy = new AutomaticPolicy(Config.L1, Config.L2, Config.T1_MS);
        measurementHistory = new MeasurementHistory(Config.HISTORY_SIZE);
        wcsUpdateSignals = new ArrayBlockingQueue<>(1);

        mode = OperatingMode.AUTOMATIC;
        tmsAvailable = true;
        waterLevel = null;
        valveOpening = 0;
        lastValidMeasurementTime = bootTime;
    }

    /**
     * Processes one TMS sample. A null value is stored but does not reset T2.
     * Invalid samples also break MID continuity because the level is unknown.
     */
    public void onMeasurement(final Double value, final long now) {
        synchronized (lock) {
            final PublicState oldState = publicStateLocked();
            final int oldValve = valveOpening;

            measurementHistory.add(new Measurement(now, value));
            waterLevel = value;

            if (value == null) {
                if (mode == OperatingMode.AUTOMATIC) {
                    automaticPolicy.reset();
                }
                publishWcsUpdateIfNeededLocked(oldState, oldValve);
                return;
            }

            final boolean recoveringFromUnconnected = !tmsAvailable;
            lastValidMeasurementTime = now;
            tmsAvailable = true;

            if (recoveringFromUnconnected) {
                mode = OperatingMode.AUTOMATIC;
                automaticPolicy.reset();
            }

            if (mode == OperatingMode.AUTOMATIC) {
                valveOpening = automaticPolicy.onMeasurement(
                    value,
                    valveOpening,
                    now
                );
            }

            publishWcsUpdateIfNeededLocked(oldState, oldValve);
        }
    }

    /** Convenience overload using the current system time. */
    public void onMeasurement(final Double value) {
        onMeasurement(value, System.currentTimeMillis());
    }

    /**
     * Periodic controller tick used for T1 and T2.
     */
    public void tick(final long now) {
        synchronized (lock) {
            final PublicState oldState = publicStateLocked();
            final int oldValve = valveOpening;

            if (tmsAvailable && now - lastValidMeasurementTime >= Config.T2_MS) {
                tmsAvailable = false;
                waterLevel = null;
                valveOpening = 5;
                automaticPolicy.reset();
            } else if (
                tmsAvailable
                    && mode == OperatingMode.AUTOMATIC
                    && waterLevel != null
            ) {
                valveOpening = automaticPolicy.onTick(valveOpening, now);
            }

            publishWcsUpdateIfNeededLocked(oldState, oldValve);
        }
    }

    /** Convenience overload using the current system time. */
    public void tick() {
        tick(System.currentTimeMillis());
    }

    /** Requests a user-selectable operating mode. */
    public CommandResult requestMode(final OperatingMode requestedMode) {
        Objects.requireNonNull(requestedMode, "requestedMode");

        synchronized (lock) {
            final SystemSnapshot before = snapshotLocked();

            if (!tmsAvailable) {
                return CommandResult.rejected(
                    "System is UNCONNECTED",
                    before
                );
            }

            if (mode == requestedMode) {
                return CommandResult.accepted(
                    false,
                    "Mode already active",
                    before
                );
            }

            final PublicState oldState = publicStateLocked();
            final int oldValve = valveOpening;

            mode = requestedMode;
            automaticPolicy.reset();

            if (mode == OperatingMode.AUTOMATIC && waterLevel != null) {
                valveOpening = automaticPolicy.onMeasurement(
                    waterLevel,
                    valveOpening,
                    System.currentTimeMillis()
                );
            }

            publishWcsUpdateIfNeededLocked(oldState, oldValve);

            return CommandResult.accepted(
                true,
                "Mode changed",
                snapshotLocked()
            );
        }
    }

    /** Requests a manual valve opening. */
    public CommandResult requestValveOpening(final int requestedOpening) {
        if (requestedOpening < 0 || requestedOpening > 100) {
            throw new IllegalArgumentException("Valve opening must be in [0,100]");
        }

        synchronized (lock) {
            final SystemSnapshot before = snapshotLocked();

            if (!tmsAvailable) {
                return CommandResult.rejected(
                    "System is UNCONNECTED",
                    before
                );
            }

            if (mode != OperatingMode.MANUAL) {
                return CommandResult.rejected(
                    "Valve can be controlled only in MANUAL mode",
                    before
                );
            }

            if (valveOpening == requestedOpening) {
                return CommandResult.accepted(
                    false,
                    "Valve opening already set",
                    before
                );
            }

            final PublicState oldState = publicStateLocked();
            final int oldValve = valveOpening;
            valveOpening = requestedOpening;

            publishWcsUpdateIfNeededLocked(oldState, oldValve);

            return CommandResult.accepted(
                true,
                "Valve opening changed",
                snapshotLocked()
            );
        }
    }

    /** Returns an immutable snapshot of the global state. */
    public SystemSnapshot getSnapshot() {
        synchronized (lock) {
            return snapshotLocked();
        }
    }

    /** Returns an independent oldest-to-newest history snapshot. */
    public List<Measurement> getMeasurementsSnapshot() {
        synchronized (lock) {
            return measurementHistory.snapshot();
        }
    }

    public void requestWcsStateUpdate() {
        wcsUpdateSignals.offer(Boolean.TRUE);
    }

    /** Waits for an update request, then reads the latest controller state. */
    public SystemSnapshot takeWcsStateUpdate() throws InterruptedException {
        wcsUpdateSignals.take();
        return getSnapshot();
    }

    private void publishWcsUpdateIfNeededLocked(final PublicState oldState, final int oldValve) {
        final PublicState newState = publicStateLocked();

        if (oldState != newState || oldValve != valveOpening) {
            wcsUpdateSignals.offer(Boolean.TRUE);
        }
    }

    private SystemSnapshot snapshotLocked() {
        return new SystemSnapshot(
            publicStateLocked(),
            valveOpening,
            waterLevel
        );
    }

    private PublicState publicStateLocked() {
        if (!tmsAvailable) {
            return PublicState.UNCONNECTED;
        }

        return mode == OperatingMode.AUTOMATIC
            ? PublicState.AUTOMATIC
            : PublicState.MANUAL;
    }

}
