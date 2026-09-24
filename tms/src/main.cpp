#include <Arduino.h>
#include "config.h"

TaskHandle_t testTaskHandle = nullptr;

void testTask(void* parameter) {
    (void) parameter;

    for (;;) {
        Serial.print("Test task is running on core ");
        Serial.println(xPortGetCoreID());

        delay(2000);
    }
}

void setup() {
    Serial.begin(SERIAL_BAUD_RATE);
    delay(1000);

    Serial.print("setup() is running on core ");
    Serial.println(xPortGetCoreID());

    xTaskCreatePinnedToCore(
        testTask,
        "TestTask",
        10000,
        nullptr,
        1,
        &testTaskHandle,
        0
    );
}

void loop() {
    Serial.print("loop() is running on core ");
    Serial.println(xPortGetCoreID());

    delay(5000);
}