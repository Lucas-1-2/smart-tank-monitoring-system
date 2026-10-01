#ifndef __POTENTIOMETER__
#define __POTENTIOMETER__

/**
 * @brief Analog potentiometer device.
 */
class Potentiometer {

public:
    /**
     * @brief Creates a potentiometer connected to the specified analog pin.
     * @param pin Analog input pin.
     */
    Potentiometer(int pin);

    /**
     * @brief Samples the physical potentiometer.
     */
    void sync();

    /**
     * @brief Returns the last sampled normalized value.
     * @return Value in the range 0.0 .. 1.0.
     */
    float getValue();

private:
    int pin;
    float value;
};

#endif
