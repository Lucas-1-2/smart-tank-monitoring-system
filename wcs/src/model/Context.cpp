#include "Context.h"

Context::Context()
    : stateAvailable(false),
      mode(AUTOMATIC),
      valveOpening(0) {
}

bool Context::isStateAvailable() {
    return stateAvailable;
}

SystemMode Context::getMode() {
    return mode;
}

int Context::getValveOpening() {
    return valveOpening;
}

void Context::setSystemState(SystemMode mode, int valveOpening) {
    this->mode = mode;
    this->valveOpening = valveOpening;
    this->stateAvailable = true;
}

const char* toString(SystemMode mode) {
    switch (mode) {
        case AUTOMATIC:
            return "AUTOMATIC";

        case MANUAL:
            return "MANUAL";

        case UNCONNECTED:
            return "UNCONNECTED";

        default:
            return "UNKNOWN";
    }
}
