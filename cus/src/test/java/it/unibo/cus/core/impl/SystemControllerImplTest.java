package it.unibo.cus.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.cus.Config;
import it.unibo.cus.core.CommandResult;
import it.unibo.cus.model.OperatingMode;
import it.unibo.cus.model.PublicState;
import it.unibo.cus.model.SystemSnapshot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class SystemControllerImplTest {

    @Test
    void startsAutomaticAndBecomesUnconnectedAfterT2WithoutValidSamples() {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);

        assertEquals(PublicState.AUTOMATIC, controller.getSnapshot().state());
        assertEquals(0, controller.getSnapshot().valveOpening());
        assertNull(controller.getSnapshot().waterLevel());

        controller.tick(boot + Config.T2_MS);
        
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());
        assertEquals(5, controller.getSnapshot().valveOpening());
    }

    @Test
    void nullSampleIsStoredButDoesNotResetT2() {
        final long boot = 10_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);

        controller.onMeasurement(null, boot + Config.T2_MS / 2);
        assertEquals(1, controller.getMeasurementsSnapshot().size());
        assertNull(controller.getMeasurementsSnapshot().get(0).value());

        controller.tick(boot + Config.T2_MS);
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());
    }

    @Test
    void validSampleRecoversToAutomaticImmediately() {
        final long boot = 20_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);

        controller.tick(boot + Config.T2_MS);
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());

        controller.onMeasurement(90.0, boot + Config.T2_MS + 1);

        assertEquals(PublicState.AUTOMATIC, controller.getSnapshot().state());
        assertEquals(100, controller.getSnapshot().valveOpening());
        assertEquals(90.0, controller.getSnapshot().waterLevel());
    }

    @Test
    void manualCommandsAreRejectedWhenUnconnected() {
        final long boot = 30_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);
        controller.tick(boot + Config.T2_MS);

        final CommandResult modeResult = controller.requestMode(OperatingMode.MANUAL);
        final CommandResult valveResult = controller.requestValveOpening(80);

        assertFalse(modeResult.accepted());
        assertFalse(valveResult.accepted());
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());
        assertEquals(5, controller.getSnapshot().valveOpening());
    }

    @Test
    void manualValveRequestWorksOnlyInManualMode() {
        final SystemControllerImpl controller = new SystemControllerImpl(0L);
        controller.onMeasurement(20.0, 1L);

        assertFalse(controller.requestValveOpening(70).accepted());

        assertTrue(controller.requestMode(OperatingMode.MANUAL).accepted());
        assertTrue(controller.requestValveOpening(70).accepted());
        assertEquals(70, controller.getSnapshot().valveOpening());
    }

    @Test
    void manualModePreservesValveAndIgnoresAutomaticUpdates() {
        final long now = System.currentTimeMillis();
        final SystemControllerImpl controller = new SystemControllerImpl(now);

        controller.onMeasurement(90.0, now);
        assertEquals(100, controller.getSnapshot().valveOpening());

        controller.requestMode(OperatingMode.MANUAL);
        assertEquals(PublicState.MANUAL, controller.getSnapshot().state());
        assertEquals(100, controller.getSnapshot().valveOpening());

        controller.onMeasurement(20.0, now + 1);
        assertEquals(20.0, controller.getSnapshot().waterLevel());
        assertEquals(100, controller.getSnapshot().valveOpening());

        controller.requestValveOpening(60);
        assertEquals(60, controller.getSnapshot().valveOpening());
    }

    @Test
    void recoveryFromManualAlwaysReturnsToAutomatic() {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);

        controller.onMeasurement(20.0, boot + 1);
        controller.requestMode(OperatingMode.MANUAL);
        controller.requestValveOpening(80);
        assertEquals(PublicState.MANUAL, controller.getSnapshot().state());
        assertEquals(80, controller.getSnapshot().valveOpening());

        controller.tick(boot + 1 + Config.T2_MS);
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());
        assertEquals(5, controller.getSnapshot().valveOpening());

        controller.onMeasurement(90.0, boot + 2 + Config.T2_MS);
        assertEquals(PublicState.AUTOMATIC, controller.getSnapshot().state());
        assertEquals(100, controller.getSnapshot().valveOpening());
    }

    @ParameterizedTest
    @CsvSource({
        "MANUAL,    20,   0, 0",
        "MANUAL,    60,   5, 0",
        "MANUAL,    90, 100, 0",
        "AUTOMATIC, 20,   0, 0",
        "AUTOMATIC, 60,   5, 0",
        "AUTOMATIC, 90, 100, 0",
        "MANUAL,    20,   0, 1",
        "MANUAL,    60,   5, 1",
        "MANUAL,    90, 100, 1",
        "AUTOMATIC, 20,   0, 1",
        "AUTOMATIC, 60,   5, 1",
        "AUTOMATIC, 90, 100, 1"
    })
    void recoveryAfterT2MatchesWithAndWithoutTimeoutTick(
        final OperatingMode previousMode,
        final double level,
        final int expectedOpening,
        final long delayAfterT2
    ) {
        final long boot = 1_000L;
        final long recoveryTime = boot + Config.T2_MS + delayAfterT2;
        final SystemControllerImpl withTick = new SystemControllerImpl(boot);
        final SystemControllerImpl withoutTick = new SystemControllerImpl(boot);

        for (final SystemControllerImpl controller : new SystemControllerImpl[] {
            withTick, withoutTick
        }) {
            controller.onMeasurement(90.0, boot);
            if (previousMode == OperatingMode.MANUAL) {
                controller.requestMode(OperatingMode.MANUAL);
                controller.requestValveOpening(80);
            }
        }

        withTick.tick(recoveryTime);
        assertEquals(
            new SystemSnapshot(PublicState.UNCONNECTED, 5, null),
            withTick.getSnapshot()
        );

        withTick.onMeasurement(level, recoveryTime);
        withoutTick.onMeasurement(level, recoveryTime);
        final SystemSnapshot expected =
            new SystemSnapshot(PublicState.AUTOMATIC, expectedOpening, level);
        assertEquals(expected, withTick.getSnapshot());
        assertEquals(expected, withoutTick.getSnapshot());
        assertEquals(
            withTick.getMeasurementsSnapshot(),
            withoutTick.getMeasurementsSnapshot()
        );

        final long beforeT1 = recoveryTime + Config.T1_MS - 1;
        withTick.onMeasurement(level, beforeT1);
        withoutTick.onMeasurement(level, beforeT1);
        assertEquals(expected, withTick.getSnapshot());
        assertEquals(expected, withoutTick.getSnapshot());

        withTick.tick(recoveryTime + Config.T1_MS);
        withoutTick.tick(recoveryTime + Config.T1_MS);
        final int openingAfterT1 = level > Config.L1 && level <= Config.L2
            ? 50 : expectedOpening;
        final SystemSnapshot afterT1 =
            new SystemSnapshot(PublicState.AUTOMATIC, openingAfterT1, level);
        assertEquals(afterT1, withTick.getSnapshot());
        assertEquals(afterT1, withoutTick.getSnapshot());
    }

    @ParameterizedTest
    @EnumSource(OperatingMode.class)
    void validMeasurementBeforeT2PreservesModeAndPreviousOpening(
        final OperatingMode previousMode
    ) {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);
        controller.onMeasurement(90.0, boot);
        if (previousMode == OperatingMode.MANUAL) {
            controller.requestMode(OperatingMode.MANUAL);
            controller.requestValveOpening(80);
        }

        controller.onMeasurement(60.0, boot + Config.T2_MS - 1);

        final PublicState expectedState = previousMode == OperatingMode.MANUAL
            ? PublicState.MANUAL : PublicState.AUTOMATIC;
        final int expectedOpening = previousMode == OperatingMode.MANUAL ? 80 : 100;
        assertEquals(
            new SystemSnapshot(expectedState, expectedOpening, 60.0),
            controller.getSnapshot()
        );
    }

    @Test
    void recoveryWithoutNetStateChangeDoesNotRequestWcsUpdate()
        throws InterruptedException {

        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);
        controller.onMeasurement(90.0, boot);
        assertEquals(controller.getSnapshot(), controller.takeWcsStateUpdate());

        controller.onMeasurement(90.0, boot + Config.T2_MS);
        assertEquals(
            new SystemSnapshot(PublicState.AUTOMATIC, 100, 90.0),
            controller.getSnapshot()
        );

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<SystemSnapshot> update =
                executor.submit(controller::takeWcsStateUpdate);
            assertThrows(TimeoutException.class, () -> update.get(100, TimeUnit.MILLISECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void nullMeasurementAfterT2KeepsPreviousOpeningUntilTick() {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);
        controller.onMeasurement(90.0, boot);

        controller.onMeasurement(null, boot + Config.T2_MS);
        assertEquals(
            new SystemSnapshot(PublicState.AUTOMATIC, 100, null),
            controller.getSnapshot()
        );

        controller.tick(boot + Config.T2_MS);
        assertEquals(
            new SystemSnapshot(PublicState.UNCONNECTED, 5, null),
            controller.getSnapshot()
        );
    }

    @Test
    void validMeasurementRestartsT2() {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);
        final long sampleTime = boot + Config.T2_MS / 2;

        controller.onMeasurement(30.0, sampleTime);
        controller.tick(boot + Config.T2_MS);
        assertEquals(PublicState.AUTOMATIC, controller.getSnapshot().state());

        controller.tick(sampleTime + Config.T2_MS);
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());
        assertEquals(5, controller.getSnapshot().valveOpening());
    }

    @Test
    void nullMeasurementResetsAutomaticPolicyTimer() {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);

        controller.onMeasurement(80.0, boot);
        controller.onMeasurement(60.0, boot + 100);
        assertEquals(100, controller.getSnapshot().valveOpening());

        controller.onMeasurement(null, boot + 200);
        controller.onMeasurement(60.0, boot + 300);
        controller.onMeasurement(60.0, boot + 100 + Config.T1_MS);
        assertEquals(100, controller.getSnapshot().valveOpening());

        controller.onMeasurement(60.0, boot + 300 + Config.T1_MS);
        assertEquals(50, controller.getSnapshot().valveOpening());
    }

    @Test
    void nullMeasurementStillClearsHighRetentionAtL2() {
        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);
        controller.onMeasurement(80.0, boot);

        controller.onMeasurement(null, boot + 100);
        assertEquals(
            new SystemSnapshot(PublicState.AUTOMATIC, 100, null),
            controller.getSnapshot()
        );

        controller.onMeasurement(Config.L2, boot + 200);
        assertEquals(100, controller.getSnapshot().valveOpening());
        controller.tick(boot + 200 + Config.T1_MS);
        assertEquals(
            new SystemSnapshot(PublicState.AUTOMATIC, 50, Config.L2),
            controller.getSnapshot()
        );
    }

    @Test
    void wcsReceivesLatestStateWithoutLosingMeasurementHistory()
        throws InterruptedException {

        final long boot = 1_000L;
        final SystemControllerImpl controller = new SystemControllerImpl(boot);

        // First valid measurement: AUTOMATIC, valve 100%.
        controller.onMeasurement(90.0, boot + 1);

        // T2 expires: UNCONNECTED, valve 5%.
        controller.tick(boot + Config.T2_MS + 1);

        assertEquals(
            PublicState.UNCONNECTED,
            controller.getSnapshot().state()
        );

        // A new valid measurement restores AUTOMATIC, valve 0%.
        controller.onMeasurement(
            20.0,
            boot + Config.T2_MS + 2
        );

        // WCS must receive the latest state, not the oldest queued one.
        final SystemSnapshot snapshot =
            controller.takeWcsStateUpdate();

        assertEquals(PublicState.AUTOMATIC, snapshot.state());
        assertEquals(0, snapshot.valveOpening());

        // Measurement history must still contain both samples.
        assertEquals(
            2,
            controller.getMeasurementsSnapshot().size()
        );
    }
}
