#ifndef __DISPLAY_TASK__
#define __DISPLAY_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include <LiquidCrystal_I2C.h>

/**
 * @brief Shows the mode and valve opening confirmed by the CUS.
 *
 * The task leaves the startup screen in place until a state is received.
 * It updates the LCD only when one of the shown values changes.
 */
class DisplayTask : public Task {

public:
    /**
     * @brief Creates the display task.
     * @param context Shared state confirmed by the CUS.
     * @param lcd LCD used to display the state.
     */
    DisplayTask(Context* context, LiquidCrystal_I2C* lcd);

    /**
     * @brief Checks the confirmed state and refreshes the LCD if needed.
     */
    void tick();

private:
    Context* context;
    LiquidCrystal_I2C* lcd;

    SystemMode lastShownMode;
    int lastShownValveOpening;
    bool firstExecution;

    /**
     * @brief Builds the text for the mode and valve rows.
     * @param mode Confirmed system mode.
     * @param valveOpening Confirmed valve opening, in percent.
     */
    void updateDisplay(SystemMode mode, int valveOpening);

    /**
     * @brief Writes a row and clears any leftover characters with spaces.
     * @param row Zero-based LCD row index.
     * @param text Text to show on the row.
     */
    void writePaddedRow(int row, const char* text);
};

#endif
