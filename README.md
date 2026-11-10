# Assignment #03 - Smart Tank Monitoring System

Implementation of **Assignment #03** for the Embedded Systems and IoT course - a.y. 2025/2026, University of Bologna.

## Overview

Smart Tank Monitoring System is an IoT system designed to monitor the rainwater level in a tank and control the opening of a water channel.

The project is composed of four interconnected subsystems:
- **Tank Monitoring Subsystem (TMS)** - ESP-based embedded system that measures the water level using an ultrasonic sensor and sends measurements via MQTT.
- **Water Channel Subsystem (WCS)** - Arduino-based embedded system that controls the valve opening and provides a local interface through a button, potentiometer and LCD display.
- **Control Unit Subsystem (CUS)** - Java-based back-end responsible for coordinating the subsystems, managing the system state and applying the control policies.
- **Dashboard Subsystem (DBS)** - Web-based interface used to monitor the system and control its operation remotely.

The system supports **automatic and manual operating modes**, with a centralised control logic managed by the CUS and an `UNCONNECTED` state to handle communication failures.

Communication between the subsystems is performed through **MQTT** (TMS-CUS), **serial connection** (WCS-CUS) and **HTTP** (DBS-CUS).

## Assignment specification

The original assignment specification is available in the official course repository:

https://github.com/pslab-unibo/esiot-2025-2026/blob/master/assignments/assignment-03.md

## Course

Embedded Systems and IoT  
Computer Science and Engineering  
University of Bologna - Cesena Campus  
Academic Year 2025/2026