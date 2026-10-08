#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>

#include "config.h"
#include "devices/Sonar.h"
#include "tasks/WaterLevelTask.h"
#include "tasks/CommunicationTask.h"
#include "data/WaterLevelData.h"
#include "devices/Led.h"

Sonar sonar(
    SONAR_TRIG_PIN,
    SONAR_ECHO_PIN,
    SONAR_TIMEOUT_US
);

WaterLevelTaskParams waterLevelTaskParams;
TaskHandle_t waterLevelTaskHandle;

CommunicationTaskParams communicationTaskParams;
TaskHandle_t communicationTaskHandle;

QueueHandle_t waterLevelQueue = nullptr;

Light* greenLed = nullptr;
Light* redLed = nullptr;

static void haltCurrentTask(const char* message) {
    Serial.println(message);
    vTaskSuspend(nullptr);
}

void setup() {
    Serial.begin(SERIAL_BAUD_RATE);

    greenLed = new Led(GREEN_LED_PIN);
    redLed = new Led(RED_LED_PIN);

    greenLed->switchOff();
    redLed->switchOn();

    waterLevelQueue = xQueueCreate(
        1,
        sizeof(WaterLevelData)
    );

    if (waterLevelQueue == nullptr) {
        haltCurrentTask("Errore durante la creazione della queue");
    }

    /* Configurazione task di misurazione */

    waterLevelTaskParams.sensor = &sonar;
    waterLevelTaskParams.tankHeightM = TANK_HEIGHT_M;
    waterLevelTaskParams.measurementPeriodMs = WATER_LEVEL_TASK_PERIOD_MS;
    waterLevelTaskParams.measurementQueue = waterLevelQueue;

    BaseType_t waterLevelTaskResult = xTaskCreatePinnedToCore(
        waterLevelTask,
        "WaterLevelTask",
        10000,
        &waterLevelTaskParams,
        1,
        &waterLevelTaskHandle,
        0
    );

    if (waterLevelTaskResult != pdPASS) {
        haltCurrentTask("Errore: WaterLevelTask non creato");
    }

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
    communicationTaskParams.measurementQueue = waterLevelQueue;
    communicationTaskParams.greenLed = greenLed;
    communicationTaskParams.redLed = redLed;

    BaseType_t communicationTaskResult = xTaskCreatePinnedToCore(
        communicationTask,
        "CommunicationTask",
        10000,
        &communicationTaskParams,
        1,
        &communicationTaskHandle,
        1
    );

    if (communicationTaskResult != pdPASS) {
        haltCurrentTask("Errore: CommunicationTask non creato");
    }
}

void loop() {
    /*
     * Tutta la logica applicativa è gestita dai task FreeRTOS creati
     * in setup(). Il task Arduino associato a loop() non è necessario.
     */
    vTaskSuspend(nullptr);
}
