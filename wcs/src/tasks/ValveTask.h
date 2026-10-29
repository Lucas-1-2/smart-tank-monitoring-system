#ifndef __VALVE_TASK__
#define __VALVE_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/ServoMotor.h"

/**
 * @brief Moves the valve to the opening confirmed by the CUS.
 *
 * The task does not use local input requests. It updates the servo only
 * when the confirmed opening changes.
 */
class ValveTask : public Task {

public:
    /**
     * @brief Creates the valve control task.
     * @param context Shared state confirmed by the CUS.
     * @param servo Servo used to move the valve.
     */
    ValveTask(Context* context, ServoMotor* servo);

    /**
     * @brief Applies a new confirmed opening to the servo if needed.
     */
    void tick();

private:
    Context* context;
    ServoMotor* servo;
    int lastAppliedOpening;

    /**
     * @brief Converts a valve opening percentage to a servo angle.
     * @param percentage Confirmed valve opening, in percent.
     * @return Servo angle between the configured closed and open angles.
     */
    int percentageToAngle(int percentage);
};

#endif
