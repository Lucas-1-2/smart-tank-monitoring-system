#include "SerialCommTask.h"
#include "kernel/MsgService.h"
#include "config.h"

SerialCommTask::SerialCommTask(Context* context)
    : context(context),
      readySent(false) {
}

void SerialCommTask::tick() {
    if (!readySent) {
        msgService.sendMsg(F("READY"));
        readySent = true;
    }

    msgService.poll();

    String receivedMessage;

    while (msgService.receiveMsg(receivedMessage)) {
        processMessage(receivedMessage);
        msgService.poll();
    }
}

void SerialCommTask::processMessage(const String& message) {
    SystemMode mode;
    int valveOpening;

    // Only valid CUS state messages can change the shared context.
    if (parseStateMessage(message, mode, valveOpening)) {
        context->setSystemState(mode, valveOpening);
    }
}

bool SerialCommTask::parseStateMessage(
    const String& message,
    SystemMode& mode,
    int& valveOpening
) {
    if (!message.startsWith("STATE,")) {
        return false;
    }

    int firstSeparator = message.indexOf(',');
    int secondSeparator = message.indexOf(',', firstSeparator + 1);

    if (firstSeparator < 0 || secondSeparator < 0) {
        return false;
    }

    if (message.indexOf(',', secondSeparator + 1) >= 0) {
        return false;
    }

    String modeText = message.substring(firstSeparator + 1, secondSeparator);
    String openingText = message.substring(secondSeparator + 1);

    modeText.trim();
    openingText.trim();

    return parseMode(modeText, mode) &&
           parsePercentage(openingText, valveOpening);
}

bool SerialCommTask::parseMode(const String& value, SystemMode& mode) {
    if (value == "AUTOMATIC") {
        mode = AUTOMATIC;
        return true;
    }

    if (value == "MANUAL") {
        mode = MANUAL;
        return true;
    }

    if (value == "UNCONNECTED") {
        mode = UNCONNECTED;
        return true;
    }

    return false;
}

bool SerialCommTask::parsePercentage(const String& value, int& percentage) {
    if (value.length() == 0) {
        return false;
    }

    for (unsigned int i = 0; i < value.length(); i++) {
        if (!isDigit(value.charAt(i))) {
            return false;
        }
    }

    long parsedValue = value.toInt();

    if (parsedValue < VALVE_MIN_PERCENT ||
        parsedValue > VALVE_MAX_PERCENT) {
        return false;
    }

    percentage = (int)parsedValue;
    return true;
}
