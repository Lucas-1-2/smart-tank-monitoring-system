# DBS - Dashboard Subsystem

Web frontend for Assignment 03 - Tank Monitoring System.

## Technology

* HTML
* CSS
* Vanilla JavaScript
* Fetch API
* Chart.js

The DBS is served directly by the CUS through the Vert.x HTTP server. No additional web server is required for the integrated system.

## Project structure

```text
dbs/
├── index.html
├── css/
│   └── style.css
├── js/
│   ├── config.js
│   ├── api.js
│   ├── chart.js
│   └── dashboard.js
└── lib/
    └── chart.umd.min.js
```

## Run

The `dbs` and `cus` folders must be placed at the same level:

```text
assignment-03/
├── cus/
├── dbs/
├── tms/
├── wcs/
└── doc/
```

Start the CUS backend and open:

```text
http://localhost:8080/
```

The dashboard can also be accessed from another device on the same network using the IP address of the PC running the CUS, for example:

```text
http://192.168.1.50:8080/
```

## Configuration

The main frontend parameters are defined in `js/config.js`:

```js
export const CONFIG = Object.freeze({
    CUS_BASE_URL: "",
    STATE_POLL_INTERVAL_MS: 1000,
    MEASUREMENTS_POLL_INTERVAL_MS: 1000,
    REQUEST_TIMEOUT_MS: 3000
});
```

`CUS_BASE_URL` is empty because the DBS and the CUS API are served by the same HTTP server. API requests therefore use relative paths.

The state and measurement polling intervals are intentionally independent.

## CUS API

The DBS expects the following HTTP endpoints:

```text
GET  /api/state
GET  /api/measurements
POST /api/mode
POST /api/valve
```

Example state response:

```json
{
  "mode": "AUTOMATIC",
  "valveOpening": 0,
  "waterLevel": 42
}
```

Possible modes are:

* `AUTOMATIC`
* `MANUAL`
* `UNCONNECTED`

`waterLevel` may be `null` when no valid measurement is available.

Example measurements response:

```json
[
  { "timestamp": 1759062000000, "value": 40 },
  { "timestamp": 1759062001000, "value": 44 },
  { "timestamp": 1759062002000, "value": null }
]
```

`null` values are displayed as gaps in the graph.

## Dashboard behaviour

The CUS is the source of truth for the global system state.

The DBS sends operator requests but updates the displayed mode and valve opening only after receiving the confirmed state from the CUS.

The mode button switches between `AUTOMATIC` and `MANUAL`.

The valve slider is enabled only in `MANUAL` mode. The new valve value is sent when the slider change is committed.

If the CUS cannot be reached, the dashboard displays `NOT AVAILABLE`, disables the operator controls and keeps the last known values visible together with the `Last update` time.

## Chart.js

Chart.js is stored locally in:

```text
lib/chart.umd.min.js
```

and loaded from `index.html` with:

```html
<script src="lib/chart.umd.min.js"></script>
```

This allows the dashboard to work without an Internet connection.
