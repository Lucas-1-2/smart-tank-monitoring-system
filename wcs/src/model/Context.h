#ifndef __CONTEXT__
#define __CONTEXT__

/**
 * @brief Global operating mode established by the CUS.
 */
enum SystemMode {
    AUTOMATIC,
    MANUAL,
    UNCONNECTED
};

/**
 * @brief Returns the text representation of a system mode.
 * @param mode Mode to convert.
 * @return Text representation of the mode.
 */
const char* toString(SystemMode mode);

/**
 * @brief Shared WCS view of the authoritative state received from the CUS.
 *
 * Local input tasks never modify this state. It is updated only when a valid
 * STATE message is received from the CUS.
 */
class Context {

public:
    Context();

    /**
     * @brief Returns whether at least one valid state has been received.
     */
    bool isStateAvailable();

    /**
     * @brief Returns the last confirmed system mode.
     */
    SystemMode getMode();

    /**
     * @brief Returns the last confirmed valve opening percentage.
     */
    int getValveOpening();

    /**
     * @brief Atomically updates the WCS view of the global state.
     * @param mode Confirmed mode received from CUS.
     * @param valveOpening Confirmed valve opening, 0..100 percent.
     */
    void setSystemState(SystemMode mode, int valveOpening);

private:
    bool stateAvailable;
    SystemMode mode;
    int valveOpening;
};

#endif
