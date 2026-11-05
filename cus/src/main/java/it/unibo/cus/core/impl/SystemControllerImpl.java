package it.unibo.cus.core.impl;

import it.unibo.cus.Config;
import it.unibo.cus.core.CommandResult;
import it.unibo.cus.core.api.AutomaticPolicy;
import it.unibo.cus.core.api.MeasurementHistory;
import it.unibo.cus.core.api.SystemController;
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
public final class SystemControllerImpl implements SystemController {

    private final Object lock;
    private final AutomaticPolicy automaticPolicy;
    private final MeasurementHistory measurementHistory;
    private final BlockingQueue<Boolean> wcsUpdateSignals;

    private OperatingMode mode;
    private boolean tmsAvailable;
    private Double waterLevel;
    private int valveOpening;
    private long lastValidMeasurementTime;

    /** Creates the controller with the current system time as boot time. */
    public SystemControllerImpl() {
        this(System.currentTimeMillis());
    }

    /**
     * Creates a controller with a fixed boot time for internal tests.
     *
     * @param bootTime the start time in milliseconds
     */
    SystemControllerImpl(final long bootTime) {
        lock = new Object();
        automaticPolicy = new AutomaticPolicyImpl(Config.L1, Config.L2, Config.T1_MS);
        measurementHistory = new MeasurementHistoryImpl(Config.HISTORY_SIZE);
        wcsUpdateSignals = new ArrayBlockingQueue<>(1);

        mode = OperatingMode.AUTOMATIC;
        tmsAvailable = true;
        waterLevel = null;
        valveOpening = 0;
        lastValidMeasurementTime = bootTime;
    }

    @Override
    public void onMeasurement(final Double value, final long now) {
        synchronized (lock) {
            final PublicState oldState = publicStateLocked();
            final int oldValve = valveOpening;

            measurementHistory.add(new Measurement(now, value));

            if (value == null) {
                waterLevel = null;
                if (mode == OperatingMode.AUTOMATIC) {
                    automaticPolicy.reset();
                }
                publishWcsUpdateIfNeededLocked(oldState, oldValve);
                return;
            }

            if (tmsAvailable && now - lastValidMeasurementTime >= Config.T2_MS) {
                enterUnconnectedLocked();
            }

            final boolean recoveringFromUnconnected = !tmsAvailable;
            lastValidMeasurementTime = now;
            tmsAvailable = true;
            waterLevel = value;

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

    @Override
    public void onMeasurement(final Double value) {
        onMeasurement(value, System.currentTimeMillis());
    }

    @Override
    public void tick(final long now) {
        synchronized (lock) {
            final PublicState oldState = publicStateLocked();
            final int oldValve = valveOpening;

            if (tmsAvailable && now - lastValidMeasurementTime >= Config.T2_MS) {
                enterUnconnectedLocked();
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

    @Override
    public void tick() {
        tick(System.currentTimeMillis());
    }

    @Override
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

    @Override
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

    @Override
    public SystemSnapshot getSnapshot() {
        synchronized (lock) {
            return snapshotLocked();
        }
    }

    @Override
    public List<Measurement> getMeasurementsSnapshot() {
        synchronized (lock) {
            return measurementHistory.snapshot();
        }
    }

    @Override
    public void requestWcsStateUpdate() {
        wcsUpdateSignals.offer(Boolean.TRUE);
    }

    @Override
    public SystemSnapshot takeWcsStateUpdate() throws InterruptedException {
        wcsUpdateSignals.take();
        return getSnapshot();
    }

    /** Applies the timeout fallback; the caller publishes only the final state. */
    private void enterUnconnectedLocked() {
        tmsAvailable = false;
        waterLevel = null;
        valveOpening = 5;
        automaticPolicy.reset();
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
