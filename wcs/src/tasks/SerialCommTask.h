#ifndef __SERIAL_COMM_TASK__
#define __SERIAL_COMM_TASK__

#include <Arduino.h>
#include "kernel/Task.h"
#include "model/Context.h"

/**
 * @brief Receives authoritative state updates from the CUS.
 */
class SerialCommTask : public Task {

public:
    /**
     * @brief Creates a serial communication task.
     *
     * @param context Pointer to the shared system context.
     */
    SerialCommTask(Context* context);

    /**
     * @brief Handles incoming serial messages from the CUS.
     */
    void tick();

private:
    Context* context;
    bool readySent;

    /**
     * @brief Processes a received serial message and updates the system state if valid.
     *
     * @param message The received serial message.
     */
    void processMessage(const String& message);

    /**
     * @brief Parses a state message received from the CUS.
     *
     * @param message The message to parse.
     * @param mode Reference where the parsed system mode is stored.
     * @param valveOpening Reference where the parsed valve opening percentage is stored.
     * @return true if the message is valid, false otherwise.
     */
    bool parseStateMessage(const String& message, SystemMode& mode, int& valveOpening);

    /**
     * @brief Parses a system mode from its textual representation.
     *
     * @param value The string containing the system mode.
     * @param mode Reference where the parsed mode is stored.
     * @return true if the mode is valid, false otherwise.
     */
    bool parseMode(const String& value, SystemMode& mode);

    /**
     * @brief Parses and validates a valve opening percentage.
     *
     * @param value The string containing the percentage value.
     * @param percentage Reference where the parsed percentage is stored.
     * @return true if the percentage is valid, false otherwise.
     */
    bool parsePercentage(const String& value, int& percentage);
};

#endif
