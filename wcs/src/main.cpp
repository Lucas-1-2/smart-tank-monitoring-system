#include <Arduino.h>
#include "config.h"
#include "kernel/Scheduler.h"
#include "kernel/MsgService.h"
#include "model/HWPlatform.h"
#include "model/Context.h"
#include "tasks/SerialCommTask.h"
#include "tasks/ButtonTask.h"
#include "tasks/PotentiometerTask.h"
#include "tasks/ValveTask.h"
#include "tasks/DisplayTask.h"

// #define __TEST_HW__

Scheduler sched;

HWPlatform* pHWPlatform;
Context* pContext;

/**
 * @brief Initializes the WCS hardware and its periodic tasks.
 *
 * In hardware test mode, the normal tasks are not started.
 */
void setup() {
    msgService.init();
    sched.init(SCHEDULER_PERIOD_MS);

    pHWPlatform = new HWPlatform();
    pHWPlatform->init();

#ifndef __TEST_HW__
    pContext = new Context();

    Task* pSerialCommTask = new SerialCommTask(pContext);
    pSerialCommTask->init(SERIAL_COMM_PERIOD_MS);
    sched.addTask(pSerialCommTask);

    Task* pButtonTask = new ButtonTask(pHWPlatform->getModeButton(), pContext);
    pButtonTask->init(BUTTON_TASK_PERIOD_MS);
    sched.addTask(pButtonTask);

    Task* pPotentiometerTask = new PotentiometerTask(pHWPlatform->getPotentiometer(), pContext);
    pPotentiometerTask->init(POTENTIOMETER_TASK_PERIOD_MS);
    sched.addTask(pPotentiometerTask);

    Task* pValveTask = new ValveTask(pContext, pHWPlatform->getServo());
    pValveTask->init(VALVE_TASK_PERIOD_MS);
    sched.addTask(pValveTask);

    Task* pDisplayTask = new DisplayTask(pContext, pHWPlatform->getLcd());
    pDisplayTask->init(DISPLAY_TASK_PERIOD_MS);
    sched.addTask(pDisplayTask);
#endif

#ifdef __TEST_HW__
    pHWPlatform->test();
#endif
}

/**
 * @brief Runs one scheduler cycle in normal mode.
 */
void loop() {
#ifndef __TEST_HW__
    sched.schedule();
#endif
}
