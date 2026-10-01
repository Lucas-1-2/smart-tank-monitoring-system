#include <Arduino.h>

#include "config.h"
#include "devices/Sonar.h"
#include "tasks/WaterLevelTask.h"
#include "tasks/CommunicationTask.h"


Sonar sonar(
    SONAR_TRIG_PIN,
    SONAR_ECHO_PIN,
    SONAR_TIMEOUT_US
);


WaterLevelTaskParams waterLevelTaskParams;
TaskHandle_t waterLevelTaskHandle;


CommunicationTaskParams communicationTaskParams;
TaskHandle_t communicationTaskHandle;


void setup() {

    Serial.begin(SERIAL_BAUD_RATE);
    delay(1000);


    /* Configurazione task di misurazione */

    waterLevelTaskParams.sensor = &sonar;
    waterLevelTaskParams.tankHeightM = TANK_HEIGHT_M;
    waterLevelTaskParams.measurementPeriodMs = WATER_LEVEL_TASK_PERIOD_MS;

    xTaskCreatePinnedToCore(
        waterLevelTask,
        "WaterLevelTask",
        10000,
        &waterLevelTaskParams,
        1,
        &waterLevelTaskHandle,
        0
    );

    delay(500);


    /* Configurazione task di comunicazione */
    communicationTaskParams.ssid = WIFI_SSID;

    communicationTaskParams.password = WIFI_PASSWORD;

    communicationTaskParams.wifiRetryDelayMs = WIFI_RETRY_DELAY_MS;

    communicationTaskParams.mqttBroker = MQTT_BROKER;

    communicationTaskParams.mqttPort = MQTT_PORT;

    communicationTaskParams.mqttClientIdPrefix = MQTT_CLIENT_ID_PREFIX;

    communicationTaskParams.mqttDataTopic = MQTT_DATA_TOPIC;

    communicationTaskParams.mqttRetryDelayMs = MQTT_RETRY_DELAY_MS;

    communicationTaskParams.taskPeriodMs = COMMUNICATION_TASK_PERIOD_MS;
    
    xTaskCreatePinnedToCore(
        communicationTask,
        "CommunicationTask",
        10000,
        &communicationTaskParams,
        1,
        &communicationTaskHandle,
        1
    );
}


void loop() {

    delay(10000);
}