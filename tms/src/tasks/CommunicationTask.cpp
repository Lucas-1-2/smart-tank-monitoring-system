#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>

#include "CommunicationTask.h"
#include "data/WaterLevelData.h"

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

enum CommunicationState {

    COMMUNICATION_ERROR,
    COMMUNICATION_OK
};


CommunicationState currentCommunicationState = COMMUNICATION_ERROR;


void setCommunicationState(
    CommunicationTaskParams* params,
    CommunicationState newState
) {

    currentCommunicationState = newState;

    switch (currentCommunicationState) {

        case COMMUNICATION_OK:

            params->redLed->switchOff();
            params->greenLed->switchOn();

            break;


        case COMMUNICATION_ERROR:

            params->greenLed->switchOff();
            params->redLed->switchOn();

            break;
    }
}


void connectToWiFi(CommunicationTaskParams* params) {

    setCommunicationState(
    params,
    COMMUNICATION_ERROR
    );

    Serial.print("Connessione alla rete Wi-Fi: ");
    Serial.println(params->ssid);

    WiFi.mode(WIFI_STA);
    WiFi.begin(params->ssid, params->password);

    while (WiFi.status() != WL_CONNECTED) {

        Serial.print(".");
        delay(params->wifiRetryDelayMs);
    }

    Serial.println();
    Serial.println("Wi-Fi connesso");

    Serial.print("Indirizzo IP: ");
    Serial.println(WiFi.localIP());
}



void connectToMqtt(CommunicationTaskParams* params) {

    setCommunicationState(
    params,
    COMMUNICATION_ERROR
    );

    while (!mqttClient.connected() &&
           WiFi.status() == WL_CONNECTED) {

        Serial.print("Connessione al broker MQTT... ");

        String clientId =
            String(params->mqttClientIdPrefix) +
            String(random(0xffff), HEX);

        if (mqttClient.connect(clientId.c_str())) {

            Serial.println("connesso");

            setCommunicationState(
            params,
            COMMUNICATION_OK
            );

        } else {

            Serial.print("fallita, stato: ");
            Serial.println(mqttClient.state());

            Serial.print("Nuovo tentativo tra ");
            Serial.print(params->mqttRetryDelayMs);
            Serial.println(" ms");

            delay(params->mqttRetryDelayMs);
        }
    }
}



void publishMeasurement(
    CommunicationTaskParams* params,
    WaterLevelData measurement
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

    bool published = mqttClient.publish(
        params->mqttDataTopic,
        payload
    );

    if (published) {

        Serial.println("Messaggio MQTT inviato");

        setCommunicationState(
        params,
        COMMUNICATION_OK
        );

    } else {

        Serial.println("Errore durante la pubblicazione MQTT");

        setCommunicationState(
        params,
        COMMUNICATION_ERROR
        );
    }
}



void communicationTask(void* parameter) {

    CommunicationTaskParams* params = static_cast<CommunicationTaskParams*>(parameter);

    randomSeed(micros());

    mqttClient.setServer(
        params->mqttBroker,
        params->mqttPort
    );

    connectToWiFi(params);
    connectToMqtt(params);

    for (;;) {

        if (WiFi.status() != WL_CONNECTED) {

            Serial.println("Connessione Wi-Fi persa");
            Serial.println("Tentativo di riconnessione...");

            connectToWiFi(params);
        }

        if (!mqttClient.connected()) {

            Serial.println("Connessione MQTT assente");

            connectToMqtt(params);
        }


        if (mqttClient.connected()) {

            if (!mqttClient.loop()) {

                setCommunicationState(
                    params,
                    COMMUNICATION_ERROR
                );
            }
        }


        if (mqttClient.connected()) {

            WaterLevelData receivedMeasurement;

            if (
                xQueueReceive(
                    params->measurementQueue,
                    &receivedMeasurement,
                    0
                ) == pdTRUE
            ) {

                publishMeasurement(
                    params,
                    receivedMeasurement
                );
            }
        }


        delay(params->taskPeriodMs);
    }
}
