# Control Unit Subsystem (CUS)

Java 17 backend for **Assignment 03 – Smart Tank Monitoring System**. The CUS receives tank measurements from the TMS, applies the control policy, exchanges commands and state updates with the WCS, and serves the DBS dashboard through HTTP.

## Overview

```text
 TMS (ESP / MQTT)                         DBS (browser)
          |                                    |
          v                                    v
   TmsMqttServiceImpl                        HttpServiceImpl
          |                                    |
          +---------------+--------------------+
                          |
                          v
                   SystemControllerImpl <------ Vert.x periodic timer
                  (authoritative state)
                          ^
                          |
                    WcsSerialAgentImpl <----> SerialCommChannelImpl <----> WCS (Arduino)
                    receiver/writer          USB serial

 SystemControllerImpl -- coalesced update signal --> WcsSerialAgentImpl
 WcsSerialAgentImpl -- read current snapshot -----> SystemControllerImpl
```

`SystemControllerImpl` owns the global state. MQTT callbacks, HTTP handlers, the serial receiver and the periodic timer use the same controller instance. State operations are protected by an internal lock; network and serial I/O take place outside that lock.

### Code organization

Each main component has an interface under `api/` and a concrete class under `impl/`.
Other components depend on the interfaces, while `RunCus` creates the implementations and connects them.
The interfaces describe what each component does. The implementation classes contain the details.

- `core`: `SystemController` manages the shared state, `AutomaticPolicy` handles T1 and the valve rules, and `MeasurementHistory` stores recent samples.
- `mqtt`: `TmsMqttService` receives TMS messages and `TmsMessageParser` reads their data.
- `serial`: `SerialCommChannel` handles serial lines, while `WcsSerialAgent` handles WCS commands and state updates.
- `http`: `HttpService` provides the REST routes and serves the DBS.

Configuration, records, enums, the application entry point and the static serial-port selector remain concrete types without extra interfaces.

**Technologies:** Java 17, Maven, Vert.x Core/Web 4.2.6, Eclipse Paho MQTT v3 1.2.5, jSerialComm 2.10.4 and JUnit 5.

## Getting started

### Requirements

- JDK **17 or newer** (the Maven project targets Java 17).
- Maven, or an IDE with a configured Maven runner.
- The **DBS** directory alongside `cus/` if you want to open the dashboard.
- Access to the MQTT broker for real TMS measurements, and an Arduino/WCS for serial communication. Both integrations can be disabled independently.

The supplied ZIP contains the example configuration under `cus/src/cus.properties.example`. **`Config.java` looks for `cus.properties` in the current working directory**, not under `src/`. Therefore, when running from `cus/`, copy the example to `cus/cus.properties`.

From the `cus/` directory, using **PowerShell**:

```powershell
Copy-Item .\src\cus.properties.example .\cus.properties
mvn clean test
mvn exec:java
```

Using **Linux/macOS**:

```bash
cp src/cus.properties.example cus.properties
mvn clean test
mvn exec:java
```

If `mvn` is not recognized by your terminal, run the Maven `test` and `exec:java` goals through your IDE's Maven integration, or configure Maven on your `PATH`. The uploaded project does **not** include Maven Wrapper (`mvnw`).

Keep the working directory set to `cus/`: configuration loading and the default relative DBS path depend on it. The local `cus.properties` file is ignored by Git.

### Run without the physical subsystems

For HTTP/DBS development, set these properties in `cus/cus.properties`:

```properties
cus.mqtt.enabled=false
cus.serial.enabled=false
```

Then run the application and open <http://localhost:8080/>. Without valid TMS samples, the controller will reach `UNCONNECTED` after T2; disabling MQTT does not disable timeout handling.

## Configuration

All configurable options are read from `cus.properties` in the process working directory. Missing values use the defaults defined in `Config.java`. The following values match the **provided example configuration**:

```properties
# Automatic valve policy
cus.l1=40
cus.l2=70
cus.t1.ms=5000
cus.t2.ms=10000

# HTTP and dashboard
cus.http.host=0.0.0.0
cus.http.port=8080
cus.dbs.dir=../dbs

# MQTT / TMS
cus.mqtt.enabled=true
cus.mqtt.broker=tcp://broker.mqtt-dashboard.com:1883
cus.mqtt.topic=unibo/esiot/2025-2026/group06/tms/data

# USB serial / WCS
cus.serial.enabled=true
cus.serial.baud=115200
# cus.serial.port=COM5
```

If `cus.serial.enabled=true` and no `cus.serial.port` is provided, the CUS displays a Swing dialog listing available serial ports. On a headless machine, specify the port explicitly. The port may be named `COM5` on Windows or `/dev/ttyACM0`/`/dev/ttyUSB0` on Linux.

**Important:** when `cus.properties` is absent, the source-level default MQTT topic in this version of `Config.java` is `smart-tank/sam-lucas/tms/data`, *not* the example's group-specific topic. Copy and check the configuration file before testing MQTT integration.

Fixed internal settings in `Config.java`:

| Parameter | Value |
| --- | --- |
| Retained measurements | Last 20 samples |
| Controller timer | Every 100 ms |
| MQTT QoS | 0 |
| Retry delay for initial MQTT connection | 5,000 ms |

`Config.validate()` checks the configuration before the services are created.

## Controller behavior

The CUS starts in `AUTOMATIC`, with valve opening `0%` and no water-level measurement yet. The public state can be `AUTOMATIC`, `MANUAL`, or `UNCONNECTED`; only `AUTOMATIC` and `MANUAL` are user-selectable.

### Automatic mode

Let `x` be the most recent valid level percentage:

| Level | Automatic action |
| --- | --- |
| `x <= L1` | Set valve to **0%** |
| `L1 < x < L2` | Keep the previous opening while waiting for T1 |
| Middle range maintained for at least T1 | Set valve to **50%** |
| `x >= L2` | Set valve to **100%** |

Leaving the middle range resets the T1 waiting period. An unavailable (`NO_ECHO`) sample also resets the pending T1 interval in `AUTOMATIC`. The periodic timer can complete T1 without another measurement if the last known level remains in the middle range.

### Manual mode

A user can request `MANUAL` through HTTP (DBS) or the serial protocol (WCS). Switching to `MANUAL` preserves the current valve opening. Subsequent valid level measurements still update the displayed water level and its history, but do not override the manual opening. The controller accepts manual valve openings only between `0` and `100` inclusive, and only in `MANUAL`.

When switching back to `AUTOMATIC`, the automatic policy is reset and evaluated using the **currently stored** water level, if it is not `null` (a prior `NO_ECHO` sample sets that value to `null`).

### T2 / UNCONNECTED

The timestamp of the last **valid** TMS reading determines T2. The controller checks for expiry in its periodic tick (nominally every 100 ms). If no valid measurement has arrived for at least T2, it enters `UNCONNECTED` and sets the valve opening to **5%**, regardless of the previous operating mode. User mode and valve commands are rejected while `UNCONNECTED`.

The next valid measurement restores `AUTOMATIC` and immediately reapplies the automatic policy. `NO_ECHO` does not restart T2. The default T2 interval is 10 seconds. As the expiry is evaluated periodically, the observed transition may occur slightly after the exact threshold.

## MQTT: TMS → CUS

The CUS subscribes to the topic configured with `cus.mqtt.topic`. The parser accepts these payloads:

**Valid sample** (only `percentage` is used by the CUS):

```json
{"status":"OK","levelM":0.420,"percentage":42.0}
```

**Unavailable reading**:

```json
{"status":"NO_ECHO"}
```

For `status=OK`, `percentage` must be a JSON number in `[0,100]`. `levelM` is ignored and is not required by the parser. `NO_ECHO` is converted to Java `null`: the controller records a history entry containing `null`, but does not reset T2. Malformed JSON, unsupported status values, and invalid percentages are discarded and do **not** enter measurement history.

The service retries initial MQTT connection failures; after a successful connection it relies on Paho's automatic reconnection and attempts to resubscribe. MQTT QoS is `0`, so delivery is not guaranteed by the protocol.

## HTTP API and DBS

Vert.x serves the REST API and the static dashboard files from the **same origin**. The default URL on the CUS machine is <http://localhost:8080/>. With `cus.http.host=0.0.0.0`, other devices on the same network may also access the server through the computer's LAN IP, subject to firewall rules.

The DBS folder is configured with `cus.dbs.dir` (default `../dbs`, relative to the `cus/` working directory). It is a **separate subsystem**, not included in this CUS-only ZIP.

| Request | Purpose | Success |
| --- | --- | --- |
| `GET /api/state` | Read the latest public state | `200` JSON object |
| `GET /api/measurements` | Read retained TMS history | `200` JSON array |
| `POST /api/mode` | Request `AUTOMATIC` or `MANUAL` | `204` No Content |
| `POST /api/valve` | Set opening in `MANUAL` | `204` No Content |

Example **state** response:

```json
{"mode":"MANUAL","valveOpening":65,"waterLevel":72.0}
```

Example **history** response, oldest to newest:

```json
[
  {"timestamp":1792749600000,"value":61.0},
  {"timestamp":1792749601000,"value":null},
  {"timestamp":1792749602000,"value":64.0}
]
```

The timestamp is Unix time in milliseconds. The bounded history includes `null` readings to preserve gaps for the dashboard graph. **It is independent from the WCS notification queue** and is not persisted to disk.

Example requests, with `Content-Type: application/json`:

```http
POST /api/mode

{"mode":"MANUAL"}
```

```http
POST /api/valve

{"opening":65}
```

A successful command returns `204`, including valid commands that leave the state unchanged. Malformed JSON, missing/wrong-typed fields, unknown modes, fractional openings and openings outside `[0,100]` return `400` with a JSON error message. A valid command that conflicts with the current controller state returns `409` (for example, a valve request in `AUTOMATIC` or any mode/valve request in `UNCONNECTED`). Errors use `{"message":"..."}`.

The server does not implement authentication or TLS. Use it in a trusted lab/LAN environment; do not expose its write endpoints directly to the public Internet.

## Serial protocol: WCS ↔ CUS

The link is ASCII, newline-delimited, `115200` baud by default, 8 data bits, no parity, one stop bit. The CUS currently configures RTS/CTS hardware flow control; check compatibility with the actual Arduino/USB-serial adapter.

**WCS → CUS**:

```text
READY
CMD,MODE,MANUAL
CMD,MODE,AUTOMATIC
CMD,VALVE,65
LOG,Potentiometer released
```

**CUS → WCS**:

```text
STATE,AUTOMATIC,100
STATE,MANUAL,65
STATE,UNCONNECTED,5
```

`READY` asks for synchronization with the current CUS state. The serial receiver passes requests to `SystemControllerImpl`; Arduino does not authoritatively set the mode or valve opening. Rejected and no-op requests cause a state refresh to be requested.

**Latest-state synchronization:** instead of queueing every previous state, the controller keeps at most **one pending update signal** in an `ArrayBlockingQueue<Boolean>(1)`. Multiple pending updates are coalesced. On a signal, the serial writer reads a **fresh snapshot** and sends one `STATE,...` line. This prevents a growing FIFO of outdated snapshots and does not affect measurement history. It also means there is **no guarantee of one distinct STATE response for every rapidly issued command**, nor of delivery/acknowledgment of every intermediate state transition. Serial write errors are logged; automatic serial reconnection is not implemented.

## Project structure

```text
cus/
├── pom.xml
├── README.md
├── .gitignore
├── src/
│   ├── cus.properties.example
│   ├── main/java/it/unibo/cus/
│   │   ├── Config.java
│   │   ├── RunCus.java
│   │   ├── core/
│   │   │   ├── CommandResult.java
│   │   │   ├── api/
│   │   │   │   ├── AutomaticPolicy.java
│   │   │   │   ├── MeasurementHistory.java
│   │   │   │   └── SystemController.java
│   │   │   └── impl/
│   │   │       ├── AutomaticPolicyImpl.java
│   │   │       ├── MeasurementHistoryImpl.java
│   │   │       └── SystemControllerImpl.java
│   │   ├── model/
│   │   │   ├── Measurement.java
│   │   │   ├── OperatingMode.java
│   │   │   ├── PublicState.java
│   │   │   └── SystemSnapshot.java
│   │   ├── mqtt/
│   │   │   ├── api/
│   │   │   │   ├── TmsMessageParser.java
│   │   │   │   └── TmsMqttService.java
│   │   │   └── impl/
│   │   │       ├── TmsMessageParserImpl.java
│   │   │       └── TmsMqttServiceImpl.java
│   │   ├── serial/
│   │   │   ├── SerialPortSelector.java
│   │   │   ├── api/
│   │   │   │   ├── SerialCommChannel.java
│   │   │   │   └── WcsSerialAgent.java
│   │   │   └── impl/
│   │   │       ├── SerialCommChannelImpl.java
│   │   │       └── WcsSerialAgentImpl.java
│   │   └── http/
│   │       ├── api/
│   │       │   └── HttpService.java
│   │       └── impl/
│   │           └── HttpServiceImpl.java
│   └── test/java/it/unibo/cus/
│       ├── core/impl/
│       │   ├── AutomaticPolicyImplTest.java
│       │   └── SystemControllerImplTest.java
│       └── mqtt/impl/
│           └── TmsMessageParserImplTest.java
└── target/                       # generated build output; Git-ignored
```

The runtime configuration file must be copied or moved to **`cus/cus.properties`** when starting from `cus/`; its presence inside `src/` alone is insufficient.

## Testing and integration checklist

Run `mvn clean test` from `cus/` or use the IDE's Maven test runner. Unit tests cover the automatic policy, core controller behavior, measurement history independence from coalesced serial updates, and the MQTT JSON parser. They do **not** simulate a real MQTT broker, browser, or Arduino.

For a full end-to-end test, verify that the CUS loads the expected properties, that MQTT `OK`/`NO_ECHO` samples affect state and history correctly, that the dashboard displays state and gaps, that WCS `READY` requests a state refresh, and that MANUAL/UNCONNECTED restrictions are enforced. Also test Arduino restart, cable disconnection, broker reconnection and controlled shutdown. These are **integration checks**, not guarantees provided by the current unit tests.
