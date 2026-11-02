package it.unibo.cus.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AutomaticPolicyImplTest {

    @Test
    void lowLevelClosesValveImmediately() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);
        assertEquals(0, policy.onMeasurement(40, 80, 0));
        assertEquals(AutomaticPolicyImpl.State.LOW, policy.getState());
    }

    @Test
    void highLevelOpensValveImmediately() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);
        assertEquals(100, policy.onMeasurement(70.1, 0, 0));
        assertEquals(AutomaticPolicyImpl.State.HIGH, policy.getState());
    }

    @Test
    void reachingL2FromBelowDoesNotTriggerHigh() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(0, policy.onMeasurement(40, 0, 0));
        assertEquals(0, policy.onMeasurement(70, 0, 100));
        assertEquals(AutomaticPolicyImpl.State.MID_WAITING, policy.getState());
        assertEquals(0, policy.onTick(0, 1099));
        assertEquals(50, policy.onTick(0, 1100));
        assertEquals(50, policy.onMeasurement(70, 50, 1200));
        assertEquals(AutomaticPolicyImpl.State.MID_ACTIVE, policy.getState());
    }

    @Test
    void highIsRetainedAtL2WithoutStartingT1() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(100, policy.onMeasurement(70.1, 0, 0));
        assertEquals(100, policy.onMeasurement(70, 100, 100));
        assertEquals(AutomaticPolicyImpl.State.HIGH, policy.getState());
        assertEquals(100, policy.onTick(100, 2000));
        assertEquals(100, policy.onMeasurement(70, 100, 2100));
        assertEquals(AutomaticPolicyImpl.State.HIGH, policy.getState());
    }

    @Test
    void fallingBelowL2StartsT1AndReturningToL2DoesNotRestoreHigh() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(100, policy.onMeasurement(80, 0, 0));
        assertEquals(100, policy.onMeasurement(70, 100, 100));
        assertEquals(100, policy.onMeasurement(69.9, 100, 200));
        assertEquals(AutomaticPolicyImpl.State.MID_WAITING, policy.getState());
        assertEquals(100, policy.onMeasurement(70, 100, 500));
        assertEquals(AutomaticPolicyImpl.State.MID_WAITING, policy.getState());
        assertEquals(100, policy.onTick(100, 1199));
        assertEquals(50, policy.onTick(100, 1200));
        assertEquals(AutomaticPolicyImpl.State.MID_ACTIVE, policy.getState());
    }

    @Test
    void resetClearsHighRetentionAtL2() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(100, policy.onMeasurement(80, 0, 0));
        policy.reset();

        assertEquals(100, policy.onMeasurement(70, 100, 100));
        assertEquals(AutomaticPolicyImpl.State.MID_WAITING, policy.getState());
        assertEquals(50, policy.onTick(100, 1100));
    }

    @Test
    void midLevelKeepsPreviousOpeningUntilT1() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(100, policy.onMeasurement(80, 0, 0));
        assertEquals(100, policy.onMeasurement(60, 100, 100));
        assertEquals(100, policy.onTick(100, 1099));
        assertEquals(50, policy.onTick(100, 1100));
        assertEquals(AutomaticPolicyImpl.State.MID_ACTIVE, policy.getState());
    }

    @Test
    void leavingMidRangeResetsTheTimer() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(30, policy.onMeasurement(60, 30, 0));
        assertEquals(0, policy.onMeasurement(30, 30, 500));
        assertEquals(0, policy.onMeasurement(60, 0, 900));
        assertEquals(0, policy.onTick(0, 1899));
        assertEquals(50, policy.onTick(0, 1900));
    }

    @Test
    void midLevelActivatesAfterT1OnMeasurement() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(80, policy.onMeasurement(60, 80, 0));
        assertEquals(50, policy.onMeasurement(60, 80, 1000));
        assertEquals(AutomaticPolicyImpl.State.MID_ACTIVE, policy.getState());
    }

    @Test
    void resetRestartsMidTimer() {
        final AutomaticPolicyImpl policy = new AutomaticPolicyImpl(40, 70, 1000);

        assertEquals(80, policy.onMeasurement(60, 80, 0));

        policy.reset();

        assertEquals(AutomaticPolicyImpl.State.UNKNOWN, policy.getState());

        assertEquals(80, policy.onMeasurement(60, 80, 2000));
        assertEquals(80, policy.onTick(80, 2999));
        assertEquals(50, policy.onTick(80, 3000));
    }
}
