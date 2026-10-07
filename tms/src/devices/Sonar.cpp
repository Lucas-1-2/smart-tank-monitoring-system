#include "Sonar.h"
#include <Arduino.h>
#include "config.h"

const float SOUND_SPEED = 331.45f + 0.62f * DEFAULT_AIR_TEMPERATURE_C;

Sonar::Sonar(int trigPin, int echoPin, unsigned long timeout)
    : trigPin(trigPin), echoPin(echoPin), timeout(timeout) {

    pinMode(this->trigPin, OUTPUT);
    pinMode(this->echoPin, INPUT);

    digitalWrite(this->trigPin, LOW);
}

float Sonar::getDistance() {

    digitalWrite(trigPin, LOW);
    delayMicroseconds(3);

    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);

    digitalWrite(trigPin, LOW);

    unsigned long echoTime = pulseIn(echoPin, HIGH, timeout); //Durata dell'eco in microsecondi; 0 se la lettura va in timeout

    if (echoTime == 0) {
        return NO_ECHO_DETECTED;
    }

    float travelTime = echoTime / 1000000.0f;

    float distance = travelTime * SOUND_SPEED / 2.0f;

    return distance;
}