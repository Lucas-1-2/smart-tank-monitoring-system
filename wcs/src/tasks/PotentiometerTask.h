#ifndef __POTENTIOMETER_TASK__
#define __POTENTIOMETER_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/Potentiometer.h"

/**
 * @brief Generates manual valve requests from significant potentiometer moves.
 */
class PotentiometerTask : public Task {

public:
    PotentiometerTask(Potentiometer* potentiometer, Context* context);
    void tick();

private:
    enum PotentiometerState {
        DISABLED,
        ACTIVE
    };

    Potentiometer* potentiometer;
    Context* context;
    PotentiometerState state;
    int referencePercentage;

    int readPercentage();
    void sendValveRequest(int percentage);
};

#endif
