#ifndef __CONTEXT__
#define __CONTEXT__

/**
 * @brief Operating modes reported by the Central Unit Subsystem (CUS).
 */
enum SystemMode {
    AUTOMATIC,
    MANUAL,
    UNCONNECTED
};

/**
 * @brief Gets the protocol name of a system mode.
 * @param mode Mode to convert.
 * @return Text used to identify the mode.
 */
const char* toString(SystemMode mode);

/**
 * @brief Stores the latest system state received from the CUS.
 *
 * The CUS controls the global state. Local input tasks only send requests.
 * SerialCommTask updates this context after a valid STATE message.
 */
class Context {

public:
    /**
     * @brief Creates an empty state with no CUS update yet.
     */
    Context();

    /**
     * @brief Checks whether the CUS has sent a valid state.
     * @return true after the first valid STATE message.
     */
    bool isStateAvailable();

    /**
     * @brief Gets the last confirmed operating mode.
     * @return Operating mode reported by the CUS.
     */
    SystemMode getMode();

    /**
     * @brief Gets the last confirmed valve opening.
     * @return Valve opening from 0 to 100 percent.
     */
    int getValveOpening();

    /**
     * @brief Stores a mode and valve opening received from the CUS.
     * @param mode Confirmed operating mode.
     * @param valveOpening Confirmed opening from 0 to 100 percent.
     */
    void setSystemState(SystemMode mode, int valveOpening);

private:
    bool stateAvailable;
    SystemMode mode;
    int valveOpening;
};

#endif
