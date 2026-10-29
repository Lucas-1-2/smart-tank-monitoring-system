#ifndef __HW_PLATFORM__
#define __HW_PLATFORM__

#include "config.h"
#include "devices/Button.h"
#include "devices/Potentiometer.h"
#include "devices/ServoMotorImpl.h"
#include <LiquidCrystal_I2C.h>

/**
 * @brief Creates and provides access to the WCS hardware devices.
 *
 * The platform sets the startup outputs before any CUS state is received.
 */
class HWPlatform {

public:
    /**
     * @brief Creates the button, potentiometer, servo and LCD objects.
     */
    HWPlatform();

    /**
     * @brief Starts the LCD and servo and shows the waiting screen.
     *
     * The valve starts closed until the CUS confirms a system state.
     */
    void init();

    /**
     * @brief Runs the manual hardware test used in test mode.
     *
     * This test uses delays and is not run by the normal scheduler.
     */
    void test();

    /**
     * @brief Gets the mode button.
     * @return Pointer to the button interface.
     */
    Button* getModeButton();

    /**
     * @brief Gets the potentiometer.
     * @return Pointer to the potentiometer device.
     */
    Potentiometer* getPotentiometer();

    /**
     * @brief Gets the servo that controls the valve.
     * @return Pointer to the servo controller.
     */
    ServoMotorImpl* getServo();

    /**
     * @brief Gets the LCD device.
     * @return Pointer to the LCD driver.
     */
    LiquidCrystal_I2C* getLcd();

private:
    Button* pModeButton;
    Potentiometer* pPotentiometer;
    ServoMotorImpl* pServo;
    LiquidCrystal_I2C* pLcd;

    /**
     * @brief Prints a row and fills the remaining cells with spaces.
     * @param row Zero-based LCD row index.
     * @param text Text to show on the row.
     */
    void writePaddedRow(int row, const char* text);
};

#endif
