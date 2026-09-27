#include "Potentiometer.h"
#include <Arduino.h>

Potentiometer::Potentiometer(int pin)
    : pin(pin),
      value(0.0f) {
}

void Potentiometer::sync() {
    value = analogRead(pin) / 1023.0f;
}

float Potentiometer::getValue() {
    return value;
}
