#include <Arduino.h>

#include "config.h"
#include "devices/Sonar.h"
#include "tasks/WaterLevelTask.h"


Sonar sonar(
    SONAR_TRIG_PIN,
    SONAR_ECHO_PIN,
    SONAR_TIMEOUT_US
);

WaterLevelTaskParams waterLevelTaskParams;

TaskHandle_t waterLevelTaskHandle;


void setup() {

    Serial.begin(SERIAL_BAUD_RATE);
    delay(1000);

    waterLevelTaskParams.sensor = &sonar;
    waterLevelTaskParams.tankHeightM = TANK_HEIGHT_M;
    waterLevelTaskParams.measurementPeriodMs = MEASUREMENT_PERIOD_MS;

    xTaskCreatePinnedToCore(
        waterLevelTask,
        "WaterLevelTask",
        10000,
        &waterLevelTaskParams,
        1,
        &waterLevelTaskHandle,
        0
    );
}


void loop() {

    delay(10000);
}