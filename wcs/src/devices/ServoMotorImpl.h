#ifndef __SERVO_MOTOR_IMPL__
#define __SERVO_MOTOR_IMPL__

#include "ServoMotor.h"
#include <Arduino.h>
#include "ServoTimer2.h"

/**
 * @brief Controls a servo using Timer2.
 *
 * Timer2 is used so the scheduler can use Timer1.
 */
class ServoMotorImpl: public ServoMotor {

public:
  /**
   * @brief Creates a servo controller on the given pin.
   * @param pin Digital output pin for the servo signal.
   */
  ServoMotorImpl(int pin);

  /**
   * @brief Attaches the servo to its pin.
   */
  void on();

  /**
   * @brief Checks whether the servo is attached through this controller.
   * @return true if on() was called without a later off().
   */
  bool isOn();

  /**
   * @brief Converts an angle into a pulse width and sends it to the servo.
   * @param angle Target angle in degrees, limited to 0..180.
   */
  void setPosition(int angle);

  /**
   * @brief Detaches the servo from its pin.
   */
  void off();
    
private:
  int pin; 
  bool _on;
  ServoTimer2 motor; 
};

#endif
