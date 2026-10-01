#ifndef COMMUNICATION_TASK_H
#define COMMUNICATION_TASK_H

#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>


struct CommunicationTaskParams {

    /* Wi-Fi */
    const char* ssid;
    const char* password;
    unsigned long wifiRetryDelayMs;

    /* MQTT */
    const char* mqttBroker;
    int mqttPort;
    const char* mqttClientIdPrefix;
    const char* mqttDataTopic;
    unsigned long mqttRetryDelayMs;

    /* Periodo del task */
    unsigned long taskPeriodMs;

    QueueHandle_t measurementQueue;
};


void communicationTask(void* parameter);


#endif