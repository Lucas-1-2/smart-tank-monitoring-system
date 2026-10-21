package it.unibo.cus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.cus.Config;
import it.unibo.cus.model.OperatingMode;
import it.unibo.cus.model.PublicState;
import org.junit.jupiter.api.Test;

class SystemControllerTest {

    @Test
    void startsAutomaticAndBecomesUnconnectedAfterT2WithoutValidSamples() {
        final long boot = 1_000L;
        final SystemController controller = new SystemController(boot);

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
        final SystemController controller = new SystemController(boot);

        controller.onMeasurement(null, boot + Config.T2_MS / 2);
        assertEquals(1, controller.getMeasurementsSnapshot().size());
        assertNull(controller.getMeasurementsSnapshot().get(0).value());

        controller.tick(boot + Config.T2_MS);
        assertEquals(PublicState.UNCONNECTED, controller.getSnapshot().state());
    }

    @Test
    void validSampleRecoversToAutomaticImmediately() {
        final long boot = 20_000L;
        final SystemController controller = new SystemController(boot);

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
        final SystemController controller = new SystemController(boot);
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
        final SystemController controller = new SystemController(0L);
        controller.onMeasurement(20.0, 1L);

        assertFalse(controller.requestValveOpening(70).accepted());

        assertTrue(controller.requestMode(OperatingMode.MANUAL).accepted());
        assertTrue(controller.requestValveOpening(70).accepted());
        assertEquals(70, controller.getSnapshot().valveOpening());
    }

    @Test
    void manualModePreservesValveAndIgnoresAutomaticUpdates() {
        final long now = System.currentTimeMillis();
        final SystemController controller = new SystemController(now);

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
        final SystemController controller = new SystemController(boot);

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

    @Test
    void validMeasurementRestartsT2() {
        final long boot = 1_000L;
        final SystemController controller = new SystemController(boot);
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
        final SystemController controller = new SystemController(boot);

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
}
