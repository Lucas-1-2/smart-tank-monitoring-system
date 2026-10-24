#include <Arduino.h>

#include "WaterLevelTask.h"
#include "data/WaterLevelData.h"

void waterLevelTask(void* parameter) {
    WaterLevelTaskParams* params = static_cast<WaterLevelTaskParams*>(parameter);
    TickType_t lastWakeTime = xTaskGetTickCount();
    const TickType_t taskPeriod = pdMS_TO_TICKS(params->measurementPeriodMs);

    for (;;) {
        float distance = params->sensor->getDistance();

        WaterLevelData measurement;

        measurement.measurementValid = 
            distance >= 0.0f &&
            distance <= params->tankHeightM;

        measurement.distanceM = distance;
        measurement.waterLevelM = 0.0f;
        measurement.waterPercentage = 0.0f;

        if (!measurement.measurementValid) {
            Serial.println("Nessun eco rilevato");

        } else {
            float waterLevel = params->tankHeightM - distance;

            // if (waterLevel > params->tankHeightM) {
            //     waterLevel = params->tankHeightM;
            // }

            float waterPercentage = (waterLevel / params->tankHeightM) * 100.0f;

            measurement.waterLevelM = waterLevel;
            measurement.waterPercentage = waterPercentage;

            Serial.print("Distanza dalla superficie: ");
            Serial.print(distance);
            Serial.println(" m");

            Serial.print("Livello dell'acqua: ");
            Serial.print(waterLevel);
            Serial.print(" m - ");
            Serial.print(waterPercentage);
            Serial.println("%");
        }

        xQueueOverwrite(params->measurementQueue, &measurement);
        vTaskDelayUntil(&lastWakeTime, taskPeriod);
    }
}
