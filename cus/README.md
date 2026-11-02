# Control Unit Subsystem (CUS)

Java 17 backend for Assignment 3 - Smart Tank Monitoring System. The CUS receives water-level measurements from the TMS, controls the valve state, communicates with the WCS and provides the HTTP API for the DBS.

## Architecture

```text
TMS -- MQTT --> TmsMqttService --+                 +-- HTTP --> DBS
                                |                 |
                                v                 v
                           SystemController <-- HttpService
                                |
                      +---------+---------+
                      |                   |
                AutomaticPolicy     MeasurementHistory
                      |
                 valve rules
                                |
                                v
                         WcsSerialAgent <-- SerialCommChannel --> WCS
```

`SystemController` owns the shared state. MQTT callbacks, HTTP requests, WCS commands and the periodic timer use the same controller. State access is synchronized. Communication and I/O run outside the controller lock.

Each main component has an interface in `api/` and a class in `impl/`. `RunCus` connects the external services. `SystemControllerImpl` creates its policy and history objects. Records, enums, configuration and static utilities do not need interfaces.

## Requirements and setup

- JDK 17 or newer; Maven (or the Maven runner in your IDE).
- An MQTT broker and a WCS board for full integration tests. Both services can be disabled.
- The separate DBS folder if you want to serve the web dashboard.

From the `cus/` folder:

```bash
cp src/cus.properties.example cus.properties
mvn clean test
mvn exec:java
```

On Windows PowerShell, use `Copy-Item .\src\cus.properties.example .\cus.properties` instead of `cp`. The project does not include Maven Wrapper (`mvnw`).

`Config` loads `cus.properties` from the current working directory. Without it, the code defaults are used. Keep the working directory set to `cus/` when running the application.

To work without the physical subsystems, set:

```properties
cus.mqtt.enabled=false
cus.serial.enabled=false
```

The HTTP server is available at `http://localhost:8080/` with the default host and port. The API can run without the DBS files, but the dashboard needs the correct folder path.

## Configuration

The supplied `src/cus.properties.example` has these values:

```properties
cus.l1=40
cus.l2=70
cus.t1.ms=5000
cus.t2.ms=10000
cus.http.host=0.0.0.0
cus.http.port=8080
cus.dbs.dir=../DBS
cus.mqtt.enabled=true
cus.mqtt.broker=tcp://broker.mqttdashboard.com:1883
cus.mqtt.topic=smart-tank/sam-lucas/tms/data
cus.serial.enabled=true
cus.serial.baud=115200
# cus.serial.port=COM5
```

`cus.dbs.dir=../DBS` points to the repository dashboard folder next to `cus/` when you run from `cus/`. The default and example configuration use this path; preserve the uppercase folder name `DBS`. If no serial port is configured, a dialog asks you to choose one. On a headless machine, set `cus.serial.port` explicitly.

The internal constants are: history size 20, controller tick 100 ms, MQTT QoS 0 and initial MQTT retry delay 5 seconds. `Config.validate()` checks the configuration on startup.

## Controller behavior

The system starts in `AUTOMATIC`, with valve opening at 0% and no water-level reading yet.

| Condition | Automatic valve opening |
| --- | --- |
| Level <= L1 | 0% |
| Level > L2 | Immediately 100%; enter HIGH |
| Level = L2 while already HIGH | Keep 100%; remain HIGH |
| L1 < level <= L2 without retained HIGH, for less than T1 | Keep previous opening |
| L1 < level <= L2 without retained HIGH, for at least T1 | 50% |

Reaching L2 from below does not trigger HIGH. After exceeding L2, HIGH remains active at L2 and ends only below L2. If the level falls into the middle range, T1 starts at that point: the valve keeps 100% during the wait, then changes to 50%. Returning to exactly L2 during that wait does not restart HIGH. Resetting the automatic policy also clears HIGH retention.

The periodic timer can complete T1 without a new reading. A `NO_ECHO` sample resets the T1 wait in `AUTOMATIC` mode.

In `MANUAL`, the user can set the opening from 0 to 100%. New measurements still update the water level and history, but do not change the valve. Switching back to `AUTOMATIC` resets the policy and evaluates the current reading, if available.

If no valid measurement arrives for T2, the public state becomes `UNCONNECTED` and the valve opening becomes 5%. Mode and valve commands are rejected. The next valid reading restores `AUTOMATIC`. `NO_ECHO` does not reset T2.

## MQTT: TMS to CUS

The TMS sends JSON to the topic set in `cus.mqtt.topic`:

```json
{"status":"OK","levelM":0.420,"percentage":42.0}
```

```json
{"status":"NO_ECHO"}
```

The CUS uses `percentage` (0 to 100). `levelM` is ignored. `NO_ECHO` is stored in history as a null reading. Invalid payloads are discarded. The service retries initial connection failures and uses Paho automatic reconnection after a successful connection.

## HTTP API: DBS

The CUS serves the DBS static files and its API on the same HTTP server. The HTTP handlers also allow requests from Live Server at `localhost:5500` and `127.0.0.1:5500`.

| Endpoint | Action |
| --- | --- |
| `GET /api/state` | Current state, valve opening and water level |
| `GET /api/measurements` | Last 20 readings, oldest first |
| `POST /api/mode` | Change mode: `{"mode":"MANUAL"}` or `{"mode":"AUTOMATIC"}` |
| `POST /api/valve` | Set opening in MANUAL: `{"opening":65}` |

Successful POST commands return `204`. Invalid requests return `400`; commands not allowed by the current state return `409`. Errors contain a JSON `message` field. There is no authentication or TLS, so use the server on a trusted network.

## Serial protocol: CUS and WCS

The connection uses ASCII lines separated by `\n`, 115200 baud by default, 8 data bits, no parity, one stop bit and **no hardware flow control**.

```text
WCS -> CUS: READY
WCS -> CUS: CMD,MODE,MANUAL
WCS -> CUS: CMD,MODE,AUTOMATIC
WCS -> CUS: CMD,VALVE,65
WCS -> CUS: LOG,debug message

CUS -> WCS: STATE,AUTOMATIC,100
CUS -> WCS: STATE,MANUAL,65
CUS -> WCS: STATE,UNCONNECTED,5
```

The controller is the source of truth. `READY` requests a state refresh. State changes are grouped into one pending update signal; the serial writer reads the latest snapshot before sending it. Intermediate states can be skipped. The serial link has no automatic reconnection or delivery acknowledgment.

## Tests

Run `mvn clean test` from `cus/`. Unit tests cover the automatic policy, controller state and timeouts, WCS update signals and MQTT parsing. Test the real broker, WCS and DBS separately for full integration.
