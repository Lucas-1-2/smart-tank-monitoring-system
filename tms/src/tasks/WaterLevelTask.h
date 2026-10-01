#ifndef WATER_LEVEL_TASK_H
#define WATER_LEVEL_TASK_H

#include "devices/DistanceSensor.h"

struct WaterLevelTaskParams {
    DistanceSensor* sensor;
    float tankHeightM;
    unsigned long measurementPeriodMs;
};

void waterLevelTask(void* parameter);

#endif