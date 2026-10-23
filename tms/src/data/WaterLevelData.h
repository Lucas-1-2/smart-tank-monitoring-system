#ifndef WATER_LEVEL_DATA_H
#define WATER_LEVEL_DATA_H

struct WaterLevelData {

    bool measurementValid;

    float distanceM;
    float waterLevelM;
    float waterPercentage;
};

#endif