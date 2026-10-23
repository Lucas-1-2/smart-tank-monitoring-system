#ifndef __HW_PLATFORM__
#define __HW_PLATFORM__

#include "config.h"
#include "devices/Button.h"
#include "devices/Potentiometer.h"
#include "devices/ServoMotorImpl.h"
#include <LiquidCrystal_I2C.h>

/**
 * @brief Creates and exposes the physical devices of the WCS.
 */
class HWPlatform {

public:
    HWPlatform();

    /**
     * @brief Initializes all hardware devices and startup outputs.
     */
    void init();

    /**
     * @brief Runs a simple sequential hardware test.
     */
    void test();

    Button* getModeButton();
    Potentiometer* getPotentiometer();
    ServoMotorImpl* getServo();
    LiquidCrystal_I2C* getLcd();

private:
    Button* pModeButton;
    Potentiometer* pPotentiometer;
    ServoMotorImpl* pServo;
    LiquidCrystal_I2C* pLcd;

    void writePaddedRow(int row, const char* text);
};

#endif
