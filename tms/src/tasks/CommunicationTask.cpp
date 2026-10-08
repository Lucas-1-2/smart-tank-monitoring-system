#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>

#include "CommunicationTask.h"
#include "data/WaterLevelData.h"

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

enum CommunicationState {
    WIFI_CONNECTING,
    MQTT_CONNECTING,
    WAITING_FIRST_PUBLISH,
    COMMUNICATION_OK
};

static void updateLeds(
    CommunicationTaskParams* params,
    CommunicationState state
) {
    if (state == COMMUNICATION_OK) {
        params->redLed->switchOff();
        params->greenLed->switchOn();
    } else {
        params->greenLed->switchOff();
        params->redLed->switchOn();
    }
}

static void setCommunicationState(
    CommunicationTaskParams* params,
    CommunicationState& currentState,
    CommunicationState newState
) {
    if (currentState == newState) {
        return;
    }

    currentState = newState;
    updateLeds(params, currentState);
}

static void startWiFiConnection(CommunicationTaskParams* params) {
    Serial.print("Connessione alla rete Wi-Fi: ");
    Serial.println(params->ssid);

    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(true);
    WiFi.begin(params->ssid, params->password);

    // Wait for connection
    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        delay(params->wifiRetryDelayMs);
    }
    Serial.println("\nConnected!");
}

static bool tryConnectMqtt(CommunicationTaskParams* params) {
    Serial.print("Connessione al broker MQTT... ");

    String clientId =
        String(params->mqttClientIdPrefix) +
        String(random(0xffff), HEX);

    if (mqttClient.connect(clientId.c_str())) {
        Serial.println("connesso");
        return true;
    }

    Serial.print("fallita, stato: ");
    Serial.println(mqttClient.state());
    return false;
}

static bool publishMeasurement(
    CommunicationTaskParams* params,
    const WaterLevelData& measurement
) {
    char payload[128];

    if (measurement.measurementValid) {
        snprintf(
            payload,
            sizeof(payload),
            "{\"status\":\"OK\",\"levelM\":%.3f,\"percentage\":%.1f}",
            measurement.waterLevelM,
            measurement.waterPercentage
        );
    } else {
        snprintf(
            payload,
            sizeof(payload),
            "{\"status\":\"NO_ECHO\"}"
        );
    }

    Serial.print("Pubblicazione MQTT: ");
    Serial.println(payload);

    const bool published = mqttClient.publish(
        params->mqttDataTopic,
        payload
    );

    if (published) {
        Serial.println("Messaggio MQTT inviato");
    } else {
        Serial.println("Errore durante la pubblicazione MQTT");
    }

    return published;
}

void communicationTask(void* parameter) {
    CommunicationTaskParams* params =
        static_cast<CommunicationTaskParams*>(parameter);

    randomSeed(micros());

    mqttClient.setServer(
        params->mqttBroker,
        params->mqttPort
    );

    CommunicationState state = WIFI_CONNECTING;
    updateLeds(params, state);

    startWiFiConnection(params);

    unsigned long lastWiFiStatusLogMs = 0;
    unsigned long lastMqttAttemptMs = 0;

    TickType_t lastWakeTime = xTaskGetTickCount();
    const TickType_t taskPeriod =
        pdMS_TO_TICKS(params->taskPeriodMs);

    for (;;) {
        const unsigned long now = millis();

        switch (state) {

            case WIFI_CONNECTING: {
                if (WiFi.status() == WL_CONNECTED) {
                    Serial.println();
                    Serial.println("Wi-Fi connesso");
                    Serial.print("Indirizzo IP: ");
                    Serial.println(WiFi.localIP());

                    lastMqttAttemptMs = 0;
                    setCommunicationState(
                        params,
                        state,
                        MQTT_CONNECTING
                    );

                } else if (
                    now - lastWiFiStatusLogMs >=
                    params->wifiRetryDelayMs
                ) {
                    Serial.println("In attesa della connessione Wi-Fi...");
                    lastWiFiStatusLogMs = now;
                }

                break;
            }

            case MQTT_CONNECTING: {
                if (WiFi.status() != WL_CONNECTED) {
                    Serial.println("Connessione Wi-Fi persa");
                    startWiFiConnection(params);
                    lastWiFiStatusLogMs = now;

                    setCommunicationState(
                        params,
                        state,
                        WIFI_CONNECTING
                    );
                    break;
                }

                if (mqttClient.connected()) {
                    setCommunicationState(
                        params,
                        state,
                        WAITING_FIRST_PUBLISH
                    );
                    break;
                }

                if (
                    lastMqttAttemptMs == 0 ||
                    now - lastMqttAttemptMs >=
                        params->mqttRetryDelayMs
                ) {
                    lastMqttAttemptMs = now;

                    if (tryConnectMqtt(params)) {
                        setCommunicationState(
                            params,
                            state,
                            WAITING_FIRST_PUBLISH
                        );
                    }
                }

                break;
            }

            case WAITING_FIRST_PUBLISH:
            case COMMUNICATION_OK: {
                if (WiFi.status() != WL_CONNECTED) {
                    Serial.println("Connessione Wi-Fi persa");
                    mqttClient.disconnect();
                    startWiFiConnection(params);
                    lastWiFiStatusLogMs = now;

                    setCommunicationState(
                        params,
                        state,
                        WIFI_CONNECTING
                    );
                    break;
                }

                if (!mqttClient.connected()) {
                    Serial.println("Connessione MQTT persa");
                    lastMqttAttemptMs = 0;

                    setCommunicationState(
                        params,
                        state,
                        MQTT_CONNECTING
                    );
                    break;
                }

                if (!mqttClient.loop()) {
                    Serial.println("Errore nella gestione della connessione MQTT");
                    mqttClient.disconnect();
                    lastMqttAttemptMs = 0;

                    setCommunicationState(
                        params,
                        state,
                        MQTT_CONNECTING
                    );
                    break;
                }

                WaterLevelData receivedMeasurement;

                if (
                    xQueueReceive(
                        params->measurementQueue,
                        &receivedMeasurement,
                        0
                    ) == pdTRUE
                ) {
                    if (
                        publishMeasurement(
                            params,
                            receivedMeasurement
                        )
                    ) {
                        setCommunicationState(
                            params,
                            state,
                            COMMUNICATION_OK
                        );
                    } else {
                        mqttClient.disconnect();
                        lastMqttAttemptMs = 0;

                        setCommunicationState(
                            params,
                            state,
                            MQTT_CONNECTING
                        );
                    }
                }

                break;
            }
        }

        vTaskDelayUntil(
            &lastWakeTime,
            taskPeriod
        );
    }
}
