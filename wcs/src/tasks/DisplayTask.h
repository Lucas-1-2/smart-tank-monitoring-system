#ifndef __DISPLAY_TASK__
#define __DISPLAY_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include <LiquidCrystal_I2C.h>

/**
 * @brief Displays only state values confirmed by the CUS.
 */
class DisplayTask : public Task {

public:
    DisplayTask(Context* context, LiquidCrystal_I2C* lcd);
    void tick();

private:
    Context* context;
    LiquidCrystal_I2C* lcd;

    SystemMode lastShownMode;
    int lastShownValveOpening;
    bool firstExecution;

    void updateDisplay(SystemMode mode, int valveOpening);
    void writePaddedRow(int row, const char* text);
};

#endif
