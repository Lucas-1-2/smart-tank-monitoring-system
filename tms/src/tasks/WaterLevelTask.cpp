#include <Arduino.h>
#include "WaterLevelTask.h"


void waterLevelTask(void* parameter) {

    WaterLevelTaskParams* params = static_cast<WaterLevelTaskParams*>(parameter);

    for (;;) {

        float distance = params->sensor->getDistance();

        bool measurementValid = distance >= 0.0f;

        if (!measurementValid) {

            Serial.println("Nessun eco rilevato");

            // TODO MQTT: comunicare NO_ECHO al CUS
            // senza inviare un livello numerico

        } else {

            float waterLevel = params->tankHeightM - distance;

            if (waterLevel < 0.0f) {
                waterLevel = 0.0f;
            }

            if (waterLevel > params->tankHeightM) {
                waterLevel = params->tankHeightM;
            }

            float waterPercentage = (waterLevel / params->tankHeightM) * 100.0f;

            Serial.print("Distanza dalla superficie: ");
            Serial.print(distance);
            Serial.println(" m");

            Serial.print("Livello dell'acqua: ");
            Serial.print(waterLevel);
            Serial.print(" m - ");
            Serial.print(waterPercentage);
            Serial.println("%");
        }

        delay(params->measurementPeriodMs);
    }
}