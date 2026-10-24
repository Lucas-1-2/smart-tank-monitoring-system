#ifndef CONFIG_H
#define CONFIG_H


#define SERIAL_BAUD_RATE 115200



/* SONAR */
#define SONAR_TRIG_PIN 5
#define SONAR_ECHO_PIN 15

#define DEFAULT_AIR_TEMPERATURE_C 20.0f

/*
 * Tempo massimo di attesa dell'eco, in microsecondi
*/
#define SONAR_TIMEOUT_US 30000UL


/*
 * Distanza tra il sonar e il fondo della tanica (in metri).
 * Poiché il sonar rappresenta il piano del 100%,
 * questa è anche l'altezza utile della tanica.
 */
#define TANK_HEIGHT_M 0.35f



/* TASK PERIODS */

/* Frequenza di esecuzione del task di misurazione */
#define WATER_LEVEL_TASK_PERIOD_MS 2000UL

/* Frequenza di controllo del task di comunicazione */
#define COMMUNICATION_TASK_PERIOD_MS 100UL

/* Intervallo tra due controlli durante la connessione */
#define WIFI_RETRY_DELAY_MS 2000UL



/* WI-FI */
#define WIFI_SSID "GUEST-FASTWEB-vjX8Y2-2G"
#define WIFI_PASSWORD "7e6943dd"



/* MQTT */

/* Broker*/
#define MQTT_BROKER "broker.mqttdashboard.com"

/*Porta MQTT*/
#define MQTT_PORT 1883

/*Prefisso dell'identificatore dell'ESP3 */
#define MQTT_CLIENT_ID_PREFIX "smart-tank-tms-"

/*Topic TMS pubblicazione misure e stati */
#define MQTT_DATA_TOPIC "smart-tank/sam-lucas/tms/data"

/*Attesa prima di un nuovo tentativo di connessione MQTT*/
#define MQTT_RETRY_DELAY_MS 5000UL



/* LED DI STATO DELLA COMUNICAZIONE */
#define GREEN_LED_PIN 25
#define RED_LED_PIN 26


#endif