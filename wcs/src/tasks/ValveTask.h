#ifndef __VALVE_TASK__
#define __VALVE_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/ServoMotor.h"

/**
 * @brief Aligns the physical valve with the opening confirmed by the CUS.
 */
class ValveTask : public Task {

public:
    ValveTask(Context* context, ServoMotor* servo);
    void tick();

private:
    Context* context;
    ServoMotor* servo;
    int lastAppliedOpening;

    int percentageToAngle(int percentage);
};

#endif
