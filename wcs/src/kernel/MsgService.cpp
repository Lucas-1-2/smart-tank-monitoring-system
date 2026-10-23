#include "MsgService.h"
#include "config.h"

MsgService msgService;

MsgService::MsgService()
    : inputBuffer(""),
      currentMsg(""),
      msgAvailable(false),
      discardingMessage(false) {
}

void MsgService::init() {
    Serial.begin(SERIAL_BAUD_RATE);

    inputBuffer.reserve(SERIAL_MAX_MESSAGE_LENGTH);
    currentMsg.reserve(SERIAL_MAX_MESSAGE_LENGTH);

    inputBuffer = "";
    currentMsg = "";
    msgAvailable = false;
    discardingMessage = false;
}

void MsgService::poll() {
    /*
     * Keep unread bytes in the hardware serial buffer while
     * a complete application message is waiting to be consumed.
     */
    while (Serial.available() > 0 && !msgAvailable) {
        int rawValue = Serial.read();

        if (rawValue < 0) {
            return;
        }

        char ch = static_cast<char>(rawValue);

        if (ch == '\r') {
            continue;
        }

        if (ch == '\n') {
            if (!discardingMessage) {
                inputBuffer.trim();

                if (inputBuffer.length() > 0) {
                    currentMsg = inputBuffer;
                    msgAvailable = true;
                }
            }

            inputBuffer = "";
            discardingMessage = false;
            continue;
        }

        if (discardingMessage) {
            continue;
        }

        if (inputBuffer.length() < SERIAL_MAX_MESSAGE_LENGTH) {
            inputBuffer += ch;
        } else {
            /*
             * Discard the complete oversized line until
             * the next newline is received.
             */
            inputBuffer = "";
            discardingMessage = true;
        }
    }
}

bool MsgService::isMsgAvailable() const {
    return msgAvailable;
}

bool MsgService::receiveMsg(String& message) {
    if (!msgAvailable) {
        return false;
    }

    message = currentMsg;
    currentMsg = "";
    msgAvailable = false;

    return true;
}

void MsgService::sendMsg(const String& message) {
    Serial.println(message);
}
