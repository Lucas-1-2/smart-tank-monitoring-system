#ifndef __BUTTON_TASK__
#define __BUTTON_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/Button.h"

/**
 * @brief Detects one mode-change request for each physical button press.
 */
class ButtonTask : public Task {

public:
    ButtonTask(Button* button, Context* context);
    void tick();

private:
    enum ButtonState {
        RELEASED,
        PRESSED
    };

    Button* button;
    Context* context;
    ButtonState state;

    void sendModeChangeRequest();
};

#endif
