/**
 * @file CommunicationTask.h
 * @brief Parameters and entry point of the communication task.
 */

#ifndef COMMUNICATION_TASK_H
#define COMMUNICATION_TASK_H

#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

#include "devices/Light.h"

/**
 * @brief Configuration and shared resources used by the communication task.
 */
struct CommunicationTaskParams {

    /* Wi-Fi */

    /** @brief SSID of the Wi-Fi network. */
    const char* ssid;

    /** @brief Password of the Wi-Fi network. */
    const char* password;

    /**
     * @brief Interval between Wi-Fi status messages, in milliseconds.
     *
     * Used while waiting for the Wi-Fi connection to be established.
     */
    unsigned long wifiRetryDelayMs;


    /* MQTT */

    /** @brief Hostname or IP address of the MQTT broker. */
    const char* mqttBroker;

    /** @brief TCP port used to connect to the MQTT broker. */
    int mqttPort;

    /**
     * @brief Prefix used to generate the MQTT client identifier.
     *
     * A random hexadecimal suffix is appended during connection attempts.
     */
    const char* mqttClientIdPrefix;

    /** @brief MQTT topic used to publish measurements and sensor status. */
    const char* mqttDataTopic;

    /** @brief Interval between MQTT connection attempts, in milliseconds. */
    unsigned long mqttRetryDelayMs;


    /* COMMUNICATION STATUS LEDS */
    
    /** @brief Green LED indicating successful MQTT publication. */
    Light* greenLed;

    /** @brief Red LED indicating that communication is not ready or has failed. */
    Light* redLed;


    /* TASK PERIOD */

    /** @brief Communication task period, in milliseconds. */
    unsigned long taskPeriodMs;

    
    /* MEASUREMENT QUEUE */

    /**
     * @brief Queue used to receive WaterLevelData from the measurement task.
     *
     * Provides the latest measurement available for publication.
     */
    QueueHandle_t measurementQueue;
};

/**
 * @brief FreeRTOS task that manages communication with the CUS through MQTT.
 *
 * Uses a finite state machine to manage Wi-Fi and MQTT connections,
 * publish queued measurements and update the communication status LEDs.
 *
 * @param parameter Pointer to a CommunicationTaskParams structure.
 *
 * @note The parameters and the referenced resources must remain valid
 *       for the entire lifetime of the task.
 */
void communicationTask(void* parameter);


#endif