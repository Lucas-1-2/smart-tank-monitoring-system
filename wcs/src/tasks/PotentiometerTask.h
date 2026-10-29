#ifndef __POTENTIOMETER_TASK__
#define __POTENTIOMETER_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/Potentiometer.h"

/**
 * @brief Sends valve requests when the potentiometer moves in MANUAL mode.
 *
 * The task has a DISABLED/ACTIVE FSM. On entry to MANUAL mode, the
 * current position becomes the reference and no request is sent.
 * Later requests need a change of at least POT_DELTA_PERCENT.
 */
class PotentiometerTask : public Task {

public:
    /**
     * @brief Creates the potentiometer task.
     * @param potentiometer Physical potentiometer.
     * @param context Shared state confirmed by the CUS.
     */
    PotentiometerTask(Potentiometer* potentiometer, Context* context);

    /**
     * @brief Tracks the operating mode and checks for significant movement.
     */
    void tick();

private:
    /** States of the potentiometer FSM. */
    enum PotentiometerState {
        DISABLED,
        ACTIVE
    };

    Potentiometer* potentiometer;
    Context* context;
    PotentiometerState state;
    int referencePercentage;

    /**
     * @brief Reads the potentiometer and converts its value to percent.
     * @return Clamped opening from 0 to 100 percent.
     */
    int readPercentage();

    /**
     * @brief Sends a valve opening request to the CUS.
     * @param percentage Requested valve opening, in percent.
     */
    void sendValveRequest(int percentage);
};

#endif
