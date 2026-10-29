#include "PotentiometerTask.h"
#include "config.h"
#include "kernel/MsgService.h"
#include <Arduino.h>

PotentiometerTask::PotentiometerTask(
    Potentiometer* potentiometer,
    Context* context
) : potentiometer(potentiometer),
    context(context),
    state(DISABLED),
    referencePercentage(0) {
}

void PotentiometerTask::tick() {
    bool manualMode =
        context->isStateAvailable() &&
        context->getMode() == MANUAL;

    switch (state) {
        case DISABLED:
            if (manualMode) {
                // Ignore the current position when MANUAL first becomes active.
                // Only later movement can send a valve request.
                referencePercentage = readPercentage();
                state = ACTIVE;
            }
            break;

        case ACTIVE:
            if (!manualMode) {
                state = DISABLED;
                break;
            }

            int currentPercentage = readPercentage();
            int delta = abs(currentPercentage - referencePercentage);

            if (delta >= POT_DELTA_PERCENT) {
                sendValveRequest(currentPercentage);
                referencePercentage = currentPercentage;
            }
            break;
    }
}

int PotentiometerTask::readPercentage() {
    potentiometer->sync();

    int percentage = (int)(potentiometer->getValue() * 100.0f + 0.5f);

    if (percentage < VALVE_MIN_PERCENT) {
        percentage = VALVE_MIN_PERCENT;
    } else if (percentage > VALVE_MAX_PERCENT) {
        percentage = VALVE_MAX_PERCENT;
    }

    return percentage;
}

void PotentiometerTask::sendValveRequest(int percentage) {
    String message = F("CMD,VALVE,");
    message += percentage;
    msgService.sendMsg(message);
}
