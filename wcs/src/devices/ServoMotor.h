#ifndef __SERVO_MOTOR__
#define __SERVO_MOTOR__

/**
 * @brief Interface for controlling a servo motor.
 */
class ServoMotor {

public:
  /**
   * @brief Enables servo control.
   */
  virtual void on() = 0;

  /**
   * @brief Checks whether servo control is enabled.
   * @return true if enabled, false otherwise.
   */
  virtual bool isOn() = 0;

  /**
   * @brief Sets the servo position.
   * @param angle Target angle in degrees.
   */
  virtual void setPosition(int angle) = 0;

  /**
   * @brief Stops controlling the servo.
   */
  virtual void off() = 0;
};

#endif
