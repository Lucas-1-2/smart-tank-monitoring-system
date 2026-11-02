#ifndef CONFIG_H
#define CONFIG_H



// =====================================================
// ================= SERIAL COMMUNICATION ==============
// =====================================================


#define SERIAL_BAUD_RATE 115200



// =====================================================
// ===================== HARDWARE ======================
// =====================================================

// SONAR
#define SONAR_TRIG_PIN 5
#define SONAR_ECHO_PIN 18


// COMMUNICATION STATUS LEDS
#define GREEN_LED_PIN 25
#define RED_LED_PIN 26



// =====================================================
// ===================== SONAR =========================
// =====================================================

// Assumed air temperature used to calculate the speed of sound (°C)
#define DEFAULT_AIR_TEMPERATURE_C 20.0f

// Echo detection timeout (microseconds)
#define SONAR_TIMEOUT_US 30000UL



// =====================================================
// ====================== TANK =========================
// =====================================================

// Distance between the sensor and the tank bottom (m).
// The sensor plane represents the 100% water level.
#define TANK_HEIGHT_M 0.30f



// =====================================================
// =================== TASK PERIODS ====================
// =====================================================

// Water level measurement task period (ms)
#define WATER_LEVEL_TASK_PERIOD_MS 2000UL

// Communication task period (ms)
#define COMMUNICATION_TASK_PERIOD_MS 100UL



// =====================================================
// ====================== WI-FI ========================
// =====================================================


#define WIFI_SSID ""
#define WIFI_PASSWORD ""

// Interval between status messages while waiting for Wi-Fi (ms)
#define WIFI_RETRY_DELAY_MS 2000UL




// =====================================================
// ======================= MQTT ========================
// =====================================================


// MQTT broker hostname or IP address
#define MQTT_BROKER "broker.mqttdashboard.com"

// MQTT broker TCP port
#define MQTT_PORT 1883

// Client identifier prefix; a random hexadecimal suffix is appended
#define MQTT_CLIENT_ID_PREFIX "smart-tank-tms-"

// Topic used to publish measurements and sensor status.
// The CUS must subscribe to this topic.
#define MQTT_DATA_TOPIC "smart-tank/sam-lucas/tms/data"

// Interval between MQTT connection attempts (ms)
#define MQTT_RETRY_DELAY_MS 5000UL



#endif