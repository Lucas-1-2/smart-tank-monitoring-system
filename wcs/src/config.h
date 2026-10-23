#ifndef __CONFIG__
#define __CONFIG__

// =====================================================
// ================= SERIAL COMMUNICATION ==============
// =====================================================

#define SERIAL_BAUD_RATE           115200UL
#define SERIAL_MAX_MESSAGE_LENGTH  48
// #define ENABLE_DEBUG_LOGS          1   // uncomment to enable LOG,... messages


// =====================================================
// ===================== HARDWARE ======================
// =====================================================

// BUTTON
// The circuit is expected to provide HIGH when pressed.
// Hardware debounce is provided by the external RC network.
#define MODE_BUTTON_PIN  8

// POTENTIOMETER
#define POTENTIOMETER_PIN  A0

// SERVO - water channel valve
#define SERVO_PIN  9

// LCD (I2C)
#define I2C_CLOCK_HZ  100000UL
#define LCD_ADDRESS   0x27
#define LCD_COLUMNS   20
#define LCD_ROWS      4


// =====================================================
// =============== SCHEDULER & TASKS ==================
// =====================================================

// Base period of scheduler (ms)
#define SCHEDULER_PERIOD_MS          50

// Tasks period (ms)
#define SERIAL_COMM_PERIOD_MS        50
#define BUTTON_TASK_PERIOD_MS        50
#define POTENTIOMETER_TASK_PERIOD_MS 100
#define VALVE_TASK_PERIOD_MS         50
#define DISPLAY_TASK_PERIOD_MS       200

static_assert(
    SERIAL_COMM_PERIOD_MS % SCHEDULER_PERIOD_MS == 0 &&
    BUTTON_TASK_PERIOD_MS % SCHEDULER_PERIOD_MS == 0 &&
    POTENTIOMETER_TASK_PERIOD_MS % SCHEDULER_PERIOD_MS == 0 &&
    VALVE_TASK_PERIOD_MS % SCHEDULER_PERIOD_MS == 0 &&
    DISPLAY_TASK_PERIOD_MS % SCHEDULER_PERIOD_MS == 0,
    "Every task period must be a multiple of the scheduler period"
);


// =====================================================
// ================= POTENTIOMETER =====================
// =====================================================

// Minimum physical movement required before a new manual valve request is sent to the CUS.
#define POT_DELTA_PERCENT  5

static_assert(
    POT_DELTA_PERCENT > 0 && POT_DELTA_PERCENT <= 100,
    "POT_DELTA_PERCENT must be in the range 1..100"
);


// =====================================================
// ======================= VALVE =======================
// =====================================================

#define VALVE_MIN_PERCENT  0
#define VALVE_MAX_PERCENT  100
#define VALVE_CLOSED_ANGLE 0
#define VALVE_OPEN_ANGLE   90

static_assert(
    VALVE_CLOSED_ANGLE >= 0 &&
    VALVE_OPEN_ANGLE <= 180 &&
    VALVE_CLOSED_ANGLE < VALVE_OPEN_ANGLE,
    "Invalid valve servo angle range"
);

#endif
