#ifndef __SERIAL_COMM_TASK__
#define __SERIAL_COMM_TASK__

#include <Arduino.h>
#include "kernel/Task.h"
#include "model/Context.h"

/**
 * @brief Receives system state messages from the CUS.
 *
 * This task sends READY once, reads complete serial messages and checks
 * their format before updating Context. Invalid messages are ignored.
 */
class SerialCommTask : public Task {

public:
    /**
     * @brief Creates the serial communication task.
     * @param context Shared WCS state to update.
     */
    SerialCommTask(Context* context);

    /**
     * @brief Sends READY once and processes available serial messages.
     */
    void tick();

private:
    Context* context;
    bool readySent;

    /**
     * @brief Updates the context only if the message is a valid STATE.
     * @param message Received serial message.
     */
    void processMessage(const String& message);

    /**
     * @brief Parses a STATE,MODE,PERCENT message.
     * @param message Received message to check.
     * @param mode Destination for the parsed mode.
     * @param valveOpening Destination for the parsed valve opening.
     * @return true if all fields are valid, false otherwise.
     */
    bool parseStateMessage(const String& message, SystemMode& mode, int& valveOpening);

    /**
     * @brief Converts a protocol mode name to a SystemMode value.
     * @param value Mode text from the message.
     * @param mode Destination for the parsed mode.
     * @return true for a known mode, false otherwise.
     */
    bool parseMode(const String& value, SystemMode& mode);

    /**
     * @brief Checks that a percentage contains only digits and is in range.
     * @param value Percentage text from the message.
     * @param percentage Destination for the parsed percentage.
     * @return true for a value from 0 to 100, false otherwise.
     */
    bool parsePercentage(const String& value, int& percentage);
};

#endif
