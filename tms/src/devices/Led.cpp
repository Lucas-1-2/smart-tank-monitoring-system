#include "Led.h"
#include <Arduino.h>

Led::Led(int pin) : pin(pin) {
    pinMode(this->pin, OUTPUT);
    switchOff();
}

void Led::switchOn() {
    digitalWrite(pin, HIGH);
}

void Led::switchOff() {
    digitalWrite(pin, LOW);
}