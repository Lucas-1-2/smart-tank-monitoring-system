#include "ServoMotorImpl.h"
#include "Arduino.h"

ServoMotorImpl::ServoMotorImpl(int pin){
  this->pin = pin;  
  _on = false;
} 

void ServoMotorImpl::on(){
  motor.attach(pin);    
  _on = true;
}

bool ServoMotorImpl::isOn(){
  return _on;
}

void ServoMotorImpl::setPosition(int angle){
	if (angle > 180){
		angle = 180;
	} else if (angle < 0){
		angle = 0;
	}
  // Map 0..180 degrees to the pulse widths used by this servo (544..2400 us).
  float coeff = (2400.0-544.0)/180;
  motor.write(544 + angle*coeff);              
}

void ServoMotorImpl::off(){
  _on = false;
  motor.detach();    
}
