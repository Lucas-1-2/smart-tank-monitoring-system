#ifndef __MSGSERVICE__
#define __MSGSERVICE__

#include <Arduino.h>

/**
 * @brief Sends and receives newline-terminated serial messages.
 *
 * Incoming characters are read without blocking. The service stores one
 * complete message at a time and discards lines that are too long.
 */
class MsgService {

public:
    /**
     * @brief Creates the serial message service.
     */
    MsgService();

    /**
     * @brief Starts serial communication and resets the input buffers.
     */
    void init();

    /**
     * @brief Reads available bytes until a full message is ready.
     */
    void poll();

    /**
     * @brief Checks if a complete message is waiting to be read.
     * @return true if a complete message is available.
     */
    bool isMsgAvailable() const;

    /**
     * @brief Gets and clears the pending message.
     * @param message Destination for the received text.
     * @return true if a message was retrieved, false otherwise.
     */
    bool receiveMsg(String& message);

    /**
     * @brief Sends one message followed by a newline.
     * @param message Message text without the final newline.
     */
    void sendMsg(const String& message);

private:
    String inputBuffer;
    String currentMsg;

    bool msgAvailable;
    bool discardingMessage;
};

/** Shared serial message service used by the tasks. */
extern MsgService msgService;

#endif
