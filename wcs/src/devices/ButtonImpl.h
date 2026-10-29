#ifndef __BUTTONIMPL__
#define __BUTTONIMPL__

#include "Button.h"

/**
 * @brief Reads a button connected to an Arduino digital pin.
 *
 * The external circuit handles debounce and gives HIGH when pressed.
 */
class ButtonImpl: public Button {
 
public:
  /**
   * @brief Creates a button on the given pin.
   * @param pin Digital input pin.
   */
  ButtonImpl(int pin);

  /**
   * @brief Reads the current pin level.
   * @return true if the input is HIGH, false otherwise.
   */
  bool isPressed();

private:
  int pin;

};

#endif
