/**
 * @file WaterLevelTask.h
 * @brief Parameters and entry point of the water level measurement task.
 */

#ifndef WATER_LEVEL_TASK_H
#define WATER_LEVEL_TASK_H

#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

#include "devices/DistanceSensor.h"

/**
 * @brief Configuration and shared resources used by the water level task.
 */
struct WaterLevelTaskParams {

    /** @brief Pointer to the sensor used to measure the distance. */
    DistanceSensor* sensor;

    /**
     * @brief Distance between the sensor and the tank bottom, in metres.
     *
     * The sensor plane represents the 100% water level.
     */
    float tankHeightM;

    /** @brief Measurement task period, in milliseconds. */
    unsigned long measurementPeriodMs;

    /**
     * @brief Queue used to send measurements to the communication task.
     *
     * Must hold one WaterLevelData element and support overwriting
     * the previous measurement with the latest one.
     */
    QueueHandle_t measurementQueue;
};

/**
 * @brief FreeRTOS task that periodically measures the water level.
 *
 * Reads the distance sensor, checks measurement validity and sends
 * the resulting data to the communication task through the queue.
 *
 * @param parameter Pointer to a WaterLevelTaskParams structure.
 *
 * @note The parameters and the referenced resources must remain valid
 *       for the entire lifetime of the task.
 */ 
void waterLevelTask(void* parameter);

#endif