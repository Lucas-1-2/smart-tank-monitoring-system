#ifndef SONAR_H
#define SONAR_H

#include "DistanceSensor.h"

#define NO_ECHO_DETECTED -1.0f

class Sonar : public DistanceSensor {

public:
    Sonar(int trigPin, int echoPin, unsigned long timeout);
    float getDistance() override;

private:
    int trigPin;
    int echoPin;
    unsigned long timeout;
};

#endif