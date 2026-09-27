# Water Channel Subsystem (WCS)

Arduino UNO implementation of the Water Channel Subsystem for Assignment 03.

## Architecture

The implementation follows the same architecture used in the Smart Drone Hangar project:

- cooperative `Scheduler`
- small shared `Context`
- `HWPlatform` for physical devices
- independent periodic tasks
- non-blocking `MsgService`

The CUS is the authoritative owner of the global state. Local WCS inputs only send requests. The LCD and servo are updated only from state values received back from the CUS.

### Tasks

- `SerialCommTask`: receives and validates CUS state updates.
- `ButtonTask`: detects one mode-change request per physical press.
- `PotentiometerTask`: in MANUAL mode, sends a new valve request when the physical value changes by at least `POT_DELTA_PERCENT`.
- `ValveTask`: applies the valve opening confirmed by the CUS.
- `DisplayTask`: displays the mode and valve opening confirmed by the CUS.

## Local FSMs

### ButtonTask

`RELEASED -> PRESSED` on a physical press. A mode-change request is sent only on this transition. In `UNCONNECTED` the press is consumed but no request is sent.

`PRESSED -> RELEASED` when the button is released.

The button is expected to be hardware-debounced through the external RC circuit. No additional time-based software debounce is applied in this version.

### PotentiometerTask

- `DISABLED`: used while the system is not in MANUAL mode.
- `ACTIVE`: entered when a CUS state confirms MANUAL mode. On entry, the current physical potentiometer position becomes the reference value without generating a command.

While ACTIVE, a request is sent only when the absolute difference from the local reference is at least `POT_DELTA_PERCENT`.

## Serial protocol

115200 baud, newline-terminated ASCII messages.

### WCS -> CUS

- `READY`
- `CMD,MODE,MANUAL`
- `CMD,MODE,AUTOMATIC`
- `CMD,VALVE,<0..100>`
- optional debug messages: `LOG,<text>`

### CUS -> WCS

- `STATE,AUTOMATIC,<0..100>`
- `STATE,MANUAL,<0..100>`
- `STATE,UNCONNECTED,<0..100>`

A `STATE` message acts as the authoritative confirmation. WCS never modifies its shared global state directly from a local input.

## Startup behaviour

Before the first valid CUS state:

- servo is initialized at 0 degrees / closed position;
- LCD shows `WAITING CUS...`;
- local button and potentiometer commands are ignored;
- WCS sends `READY` once.

## Current configurable values

All tunable values are centralized in `src/config.h`.

Current defaults:

- button pin: D8
- servo pin: D9
- potentiometer: A0
- LCD: I2C `0x27`, 20x4
- potentiometer delta: 5%
- valve range: 0% -> 0 degrees, 100% -> 90 degrees
- scheduler base period: 50 ms

`ButtonImpl` assumes that the external button circuit reads `HIGH` when pressed, consistently with the Smart Drone Hangar implementation. If the final RC circuit is active-low, invert `ButtonImpl::isPressed()` or adapt the wiring.

## Notes

The project intentionally does not implement a CUS-WCS heartbeat/watchdog because loss of the wired serial link is not part of the assignment requirements. Such a mechanism can be added later as an optional fail-safe extension.
