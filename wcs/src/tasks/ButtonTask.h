#ifndef __BUTTON_TASK__
#define __BUTTON_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/Button.h"

/**
 * @brief Sends a mode-change request on each new button press.
 *
 * This task has a two-state FSM: RELEASED and PRESSED. A held button
 * cannot send repeated requests. The CUS decides the actual mode.
 */
class ButtonTask : public Task {

public:
    /**
     * @brief Creates the button task.
     * @param button Physical mode button.
     * @param context Shared state confirmed by the CUS.
     */
    ButtonTask(Button* button, Context* context);

    /**
     * @brief Checks button transitions and sends a request on a new press.
     */
    void tick();

private:
    /** States of the button press FSM. */
    enum ButtonState {
        RELEASED,
        PRESSED
    };

    Button* button;
    Context* context;
    ButtonState state;

    /**
     * @brief Asks the CUS to switch between MANUAL and AUTOMATIC.
     *
     * No request is sent before the first STATE message or in UNCONNECTED.
     */
    void sendModeChangeRequest();
};

#endif
