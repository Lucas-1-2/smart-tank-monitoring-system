#include "Logger.h"
#include "MsgService.h"
#include "config.h"

LoggerService Logger;

void LoggerService::log(const String& message) {
#if ENABLE_DEBUG_LOGS
    String logMessage = F("LOG,");
    logMessage += message;

    msgService.sendMsg(logMessage);
#else
    (void)message;
#endif
}
