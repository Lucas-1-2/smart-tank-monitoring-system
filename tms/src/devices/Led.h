#ifndef LED_H
#define LED_H

#include "Light.h"

class Led : public Light {

public:
    Led(int pin);
    void switchOn() override;
    void switchOff() override;

private:
    int pin;
};

#endif