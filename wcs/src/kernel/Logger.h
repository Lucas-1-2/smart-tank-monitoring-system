#ifndef __LOGGER__
#define __LOGGER__

#include <Arduino.h>

/**
 * @brief Serial debug logging service.
 */
class LoggerService {

public:
    /**
     * @brief Sends a debug message when logging is enabled.
     * @param message Debug message.
     */
    void log(const String& message);
};

extern LoggerService Logger;

#endif
