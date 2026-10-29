#ifndef __BUTTON__
#define __BUTTON__

/**
 * @brief Interface for reading a physical button.
 */
class Button {
 
public:
  /**
   * @brief Checks the current button state.
   * @return true if the button is pressed, false otherwise.
   */
  virtual bool isPressed() = 0;
};

#endif
