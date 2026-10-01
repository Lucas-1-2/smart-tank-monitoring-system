#include "ValveTask.h"
#include "config.h"
#include <Arduino.h>

ValveTask::ValveTask(Context* context, ServoMotor* servo)
    : context(context),
      servo(servo),
      lastAppliedOpening(-1) {
}

void ValveTask::tick() {
    if (!context->isStateAvailable()) {
        return;
    }

    int currentOpening = context->getValveOpening();

    if (currentOpening == lastAppliedOpening) {
        return;
    }

    servo->setPosition(percentageToAngle(currentOpening));
    lastAppliedOpening = currentOpening;
}

int ValveTask::percentageToAngle(int percentage) {
    long angle = map(
        percentage,
        VALVE_MIN_PERCENT,
        VALVE_MAX_PERCENT,
        VALVE_CLOSED_ANGLE,
        VALVE_OPEN_ANGLE
    );

    return (int)angle;
}
