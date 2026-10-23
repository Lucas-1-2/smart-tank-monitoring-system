#ifndef __MSGSERVICE__
#define __MSGSERVICE__

#include <Arduino.h>

/**
 * @brief Non-blocking serial transport service.
 */
class MsgService {

public:
    MsgService();

    /**
     * @brief Initializes the serial communication.
     */
    void init();

    /**
     * @brief Reads the currently available serial characters.
     */
    void poll();

    /**
     * @brief Returns whether a complete message is available.
     */
    bool isMsgAvailable() const;

    /**
     * @brief Retrieves the pending message.
     * @param message Destination string.
     * @return true if a message was retrieved, false otherwise.
     */
    bool receiveMsg(String& message);

    /**
     * @brief Sends a newline-terminated message.
     * @param message Message to send.
     */
    void sendMsg(const String& message);

private:
    String inputBuffer;
    String currentMsg;

    bool msgAvailable;
    bool discardingMessage;
};

extern MsgService msgService;

#endif
