#ifndef __POTENTIOMETER__
#define __POTENTIOMETER__

/**
 * @brief Reads and stores the position of an analog potentiometer.
 */
class Potentiometer {

public:
    /**
     * @brief Creates a potentiometer connected to an analog pin.
     * @param pin Analog input pin.
     */
    Potentiometer(int pin);

    /**
     * @brief Reads the analog input and updates the stored value.
     */
    void sync();

    /**
     * @brief Gets the most recent sampled value.
     * @return Normalized value from 0.0 to 1.0.
     */
    float getValue();

private:
    int pin;
    float value;
};

#endif
