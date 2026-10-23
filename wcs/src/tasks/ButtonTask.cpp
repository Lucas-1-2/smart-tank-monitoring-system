#include "ButtonTask.h"
#include "kernel/MsgService.h"

ButtonTask::ButtonTask(Button* button, Context* context)
    : button(button),
      context(context),
      state(RELEASED) {
}

void ButtonTask::tick() {
    bool pressed = button->isPressed();

    switch (state) {
        case RELEASED:
            if (pressed) {
                sendModeChangeRequest();
                state = PRESSED;
            }
            break;

        case PRESSED:
            if (!pressed) {
                state = RELEASED;
            }
            break;
    }
}

void ButtonTask::sendModeChangeRequest() {
    if (!context->isStateAvailable()) {
        return;
    }

    SystemMode mode = context->getMode();

    if (mode == AUTOMATIC) {
        msgService.sendMsg(F("CMD,MODE,MANUAL"));
    } else if (mode == MANUAL) {
        msgService.sendMsg(F("CMD,MODE,AUTOMATIC"));
    }
    /*
     * In UNCONNECTED mode the request is intentionally ignored.
     * The PRESSED state still consumes the current physical press.
     */
}
