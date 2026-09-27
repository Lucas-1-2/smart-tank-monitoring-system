#include "HWPlatform.h"
#include <Arduino.h>
#include <Wire.h>
#include "devices/ButtonImpl.h"

HWPlatform::HWPlatform() {
    pModeButton = new ButtonImpl(MODE_BUTTON_PIN);
    pPotentiometer = new Potentiometer(POTENTIOMETER_PIN);
    pServo = new ServoMotorImpl(SERVO_PIN);
    pLcd = new LiquidCrystal_I2C(
        LCD_ADDRESS,
        LCD_COLUMNS,
        LCD_ROWS
    );
}

void HWPlatform::init() {
    pLcd->init();
    Wire.setClock(I2C_CLOCK_HZ);
    pLcd->backlight();

    pServo->on();
    pServo->setPosition(VALVE_CLOSED_ANGLE);

    writePaddedRow(0, "WAITING CUS...");
    writePaddedRow(1, "Valve: --%");
    writePaddedRow(2, "");
    writePaddedRow(3, "");
}

Button* HWPlatform::getModeButton() {
    return pModeButton;
}

Potentiometer* HWPlatform::getPotentiometer() {
    return pPotentiometer;
}

ServoMotorImpl* HWPlatform::getServo() {
    return pServo;
}

LiquidCrystal_I2C* HWPlatform::getLcd() {
    return pLcd;
}

void HWPlatform::writePaddedRow(int row, const char* text) {
    if (row < 0 || row >= LCD_ROWS) {
        return;
    }

    pLcd->setCursor(0, row);
    int writtenCharacters = 0;

    while (*text != '\0' && writtenCharacters < LCD_COLUMNS) {
        pLcd->print(*text);
        text++;
        writtenCharacters++;
    }

    while (writtenCharacters < LCD_COLUMNS) {
        pLcd->print(' ');
        writtenCharacters++;
    }
}

void HWPlatform::test() {
    Serial.println(F("===== WCS HARDWARE TEST ====="));

    Serial.print(F("Mode button: "));
    Serial.println(
        pModeButton->isPressed()
            ? F("PRESSED")
            : F("NOT PRESSED")
    );

    pPotentiometer->sync();
    Serial.print(F("Potentiometer: "));
    Serial.print(pPotentiometer->getValue() * 100.0f, 1);
    Serial.println(F(" %"));

    writePaddedRow(0, "WCS HW TEST");
    writePaddedRow(1, "Servo -> 0%");
    pServo->setPosition(VALVE_CLOSED_ANGLE);
    delay(1000);

    writePaddedRow(1, "Servo -> 100%");
    pServo->setPosition(VALVE_OPEN_ANGLE);
    delay(1000);

    pServo->setPosition(VALVE_CLOSED_ANGLE);
    writePaddedRow(0, "WAITING CUS...");
    writePaddedRow(1, "Valve: --%");

    Serial.println(F("===== TEST COMPLETED ====="));
}
