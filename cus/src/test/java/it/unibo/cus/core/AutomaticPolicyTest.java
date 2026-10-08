package it.unibo.cus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AutomaticPolicyTest {

    @Test
    void lowLevelClosesValveImmediately() {
        final AutomaticPolicy policy = new AutomaticPolicy(40, 70, 1000);
        assertEquals(0, policy.onMeasurement(40, 80, 0));
        assertEquals(AutomaticPolicy.State.LOW, policy.getState());
    }

    @Test
    void highLevelOpensValveImmediately() {
        final AutomaticPolicy policy = new AutomaticPolicy(40, 70, 1000);
        assertEquals(100, policy.onMeasurement(70, 0, 0));
        assertEquals(AutomaticPolicy.State.HIGH, policy.getState());
    }

    @Test
    void midLevelKeepsPreviousOpeningUntilT1() {
        final AutomaticPolicy policy = new AutomaticPolicy(40, 70, 1000);

        assertEquals(100, policy.onMeasurement(80, 0, 0));
        assertEquals(100, policy.onMeasurement(60, 100, 100));
        assertEquals(100, policy.onTick(100, 1099));
        assertEquals(50, policy.onTick(100, 1100));
        assertEquals(AutomaticPolicy.State.MID_ACTIVE, policy.getState());
    }

    @Test
    void leavingMidRangeResetsTheTimer() {
        final AutomaticPolicy policy = new AutomaticPolicy(40, 70, 1000);

        assertEquals(30, policy.onMeasurement(60, 30, 0));
        assertEquals(0, policy.onMeasurement(30, 30, 500));
        assertEquals(0, policy.onMeasurement(60, 0, 900));
        assertEquals(0, policy.onTick(0, 1899));
        assertEquals(50, policy.onTick(0, 1900));
    }

    @Test
    void midLevelActivatesAfterT1OnMeasurement() {
        final AutomaticPolicy policy = new AutomaticPolicy(40, 70, 1000);

        assertEquals(80, policy.onMeasurement(60, 80, 0));
        assertEquals(50, policy.onMeasurement(60, 80, 1000));
        assertEquals(AutomaticPolicy.State.MID_ACTIVE, policy.getState());
    }

    @Test
    void resetRestartsMidTimer() {
        final AutomaticPolicy policy = new AutomaticPolicy(40, 70, 1000);

        assertEquals(80, policy.onMeasurement(60, 80, 0));

        policy.reset();

        assertEquals(AutomaticPolicy.State.UNKNOWN, policy.getState());

        assertEquals(80, policy.onMeasurement(60, 80, 2000));
        assertEquals(80, policy.onTick(80, 2999));
        assertEquals(50, policy.onTick(80, 3000));
    }
}
