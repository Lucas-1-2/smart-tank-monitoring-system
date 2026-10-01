#include <Arduino.h>
#include <WiFi.h>

#include "CommunicationTask.h"


void connectToWiFi(CommunicationTaskParams* params) {

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


void communicationTask(void* parameter) {

    CommunicationTaskParams* params =
        static_cast<CommunicationTaskParams*>(parameter);

    connectToWiFi(params);

    for (;;) {

        if (WiFi.status() != WL_CONNECTED) {

            Serial.println("Connessione Wi-Fi persa");
            Serial.println("Tentativo di riconnessione...");

            connectToWiFi(params);
        }

        delay(params->taskPeriodMs);
    }
}