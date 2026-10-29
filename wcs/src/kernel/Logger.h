#ifndef __LOGGER__
#define __LOGGER__

#include <Arduino.h>

/**
 * @brief Optional debug logger for serial messages.
 *
 * Logs use the LOG prefix and are sent only when debug logging is enabled.
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
