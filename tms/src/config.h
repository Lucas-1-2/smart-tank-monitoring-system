#ifndef CONFIG_H
#define CONFIG_H


#define SERIAL_BAUD_RATE 115200



/* SONAR */

// TODO: verificare i pin quando avremo il modello esatto di ESP32
#define SONAR_TRIG_PIN 5
#define SONAR_ECHO_PIN 18

/*
 * TODO: la temperatura potrebbe essere letta automaticamente
 * mediante un sensore dedicato.
 * Per ora viene utilizzata una temperatura fissa per calcolare
 * la velocità del suono.
 */
#define DEFAULT_AIR_TEMPERATURE_C 20.0f

/*
 * TODO: si può scegliere di cambiare
 * Tempo massimo di attesa dell'eco, in microsecondi
*/
#define SONAR_TIMEOUT_US 30000UL


/*
 * Distanza tra il sonar e il fondo della tanica.
 * Poiché il sonar rappresenta il piano del 100%,
 * questa è anche l'altezza utile della tanica.
 *
 * TODO: sostituire con la misura reale quando avremo l'hardware.
 */
#define TANK_HEIGHT_M 1.00f



/* TASK PERIODS */

/* Frequenza di esecuzione del task di misurazione */
#define WATER_LEVEL_TASK_PERIOD_MS 2000UL

/* Frequenza di controllo del task di comunicazione */
#define COMMUNICATION_TASK_PERIOD_MS 100UL

/* Intervallo tra due controlli durante la connessione */
#define WIFI_RETRY_DELAY_MS 500UL



/* WI-FI */

/*
 * TODO: inserire le credenziali della rete utilizzata
 * durante le prove con l'ESP32.
 */
#define WIFI_SSID "INSERISCI_NOME_RETE"
#define WIFI_PASSWORD "INSERISCI_PASSWORD"



/* MQTT */

/* Broker (questa è lab prof)*/
#define MQTT_BROKER "broker.mqtt-dashboard.com"

/* Porta MQTT standard(per ora) */
#define MQTT_PORT 1883

/* Prefisso dell'identificatore dell'ESP32 */
#define MQTT_CLIENT_ID_PREFIX "smart-tank-tms-"

/* Topic sul quale il TMS pubblicherà misure e stati */
#define MQTT_DATA_TOPIC "smart-tank/sam-lucas/tms/data""

/* Attesa prima di un nuovo tentativo di connessione MQTT */
#define MQTT_RETRY_DELAY_MS 5000UL



/* LED DI STATO DELLA COMUNICAZIONE */
#define GREEN_LED_PIN 25
#define RED_LED_PIN 26


#endif