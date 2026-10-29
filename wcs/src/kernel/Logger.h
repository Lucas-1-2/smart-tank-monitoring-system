#ifndef __LOGGER__
#define __LOGGER__

#include <Arduino.h>

/**
 * @brief Provides optional debug logging through the serial interface.
 *
 * Debug messages use the "LOG," prefix and are sent through MsgService.
 * Logging can be enabled or disabled in config.h.
 *
 * This service can be used to monitor system behavior during development.
 */
class LoggerService {

public:
    /**
     * @brief Sends a log message if debugging is enabled.
     * @param message Text to include after the LOG prefix.
     */
    void log(const String& message);
};

/** Shared debug logger. */
extern LoggerService Logger;

#endif
