#ifndef WATER_LEVEL_TASK_H
#define WATER_LEVEL_TASK_H

#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

#include "devices/DistanceSensor.h"

struct WaterLevelTaskParams {
    DistanceSensor* sensor;
    float tankHeightM;
    unsigned long measurementPeriodMs;

    QueueHandle_t measurementQueue;
};

void waterLevelTask(void* parameter);

#endif