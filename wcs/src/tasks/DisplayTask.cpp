#include "DisplayTask.h"
#include "config.h"
#include <Arduino.h>

DisplayTask::DisplayTask(Context* context, LiquidCrystal_I2C* lcd)
    : context(context),
      lcd(lcd),
      lastShownMode(AUTOMATIC),
      lastShownValveOpening(-1),
      firstExecution(true) {
}

void DisplayTask::tick() {
    if (!context->isStateAvailable()) {
        return;
    }

    SystemMode currentMode = context->getMode();
    int currentValveOpening = context->getValveOpening();

    if (firstExecution ||
        currentMode != lastShownMode ||
        currentValveOpening != lastShownValveOpening) {

        updateDisplay(currentMode, currentValveOpening);

        lastShownMode = currentMode;
        lastShownValveOpening = currentValveOpening;
        firstExecution = false;
    }
}

void DisplayTask::updateDisplay(SystemMode mode, int valveOpening) {
    // Keep room for the final null character in both LCD row buffers.
    char modeRow[LCD_COLUMNS + 1];
    char valveRow[LCD_COLUMNS + 1];

    snprintf(
        modeRow,
        sizeof(modeRow),
        "Mode: %s",
        toString(mode)
    );

    snprintf(
        valveRow,
        sizeof(valveRow),
        "Valve: %d%%",
        valveOpening
    );

    writePaddedRow(0, modeRow);
    writePaddedRow(1, valveRow);
}

void DisplayTask::writePaddedRow(int row, const char* text) {
    if (row < 0 || row >= LCD_ROWS) {
        return;
    }

    lcd->setCursor(0, row);
    int writtenCharacters = 0;

    while (*text != '\0' && writtenCharacters < LCD_COLUMNS) {
        lcd->print(*text);
        text++;
        writtenCharacters++;
    }

    while (writtenCharacters < LCD_COLUMNS) {
        lcd->print(' ');
        writtenCharacters++;
    }
}
