# DBS - Dashboard Subsystem

Web frontend for Assignment 03 - Tank Monitoring System.

## Technology

* HTML
* CSS
* Vanilla JavaScript
* Fetch API
* Chart.js

The DBS shows the system state, water level and valve opening received from the CUS. It also allows the operator to request a mode change or a new valve opening.

## Project structure

```text
DBS/
├── index.html
├── css/
│   └── style.css
├── js/
│   ├── config.js
│   ├── api.js
│   ├── chart.js
│   └── dashboard.js
└── lib/
    ├── chart.umd.min.js
    └── README.txt
```

## Run

Start the CUS HTTP server on port `8080`.

The dashboard can be opened in one of these ways:

* If the CUS serves the DBS files, open `http://localhost:8080/`.
* If you use a separate local web server, such as VS Code Live Server, open the address provided by that server (usually `http://127.0.0.1:5500/` or `http://localhost:5500/`).

Do not open `index.html` directly using a `file://` address. The JavaScript files use ES modules and should be served through HTTP.

With the current configuration, the browser sends all API requests to `http://localhost:8080`, even when the DBS page is opened on another port. If the page is served from a different origin, the CUS must allow the browser requests through CORS.

**Note:** On another PC or phone, `localhost` points to that device, not to the PC running the CUS. To open the DBS from another device, change `CUS_BASE_URL` in `js/config.js` to the reachable CUS address, such as `http://192.168.1.50:8080`. The CUS must also allow requests from the page origin when needed.

## Configuration

The main frontend parameters are defined in `js/config.js`. Their current values are:

```js
export const CONFIG = Object.freeze({
    CUS_BASE_URL: "http://localhost:8080",
    STATE_POLL_INTERVAL_MS: 1000,
    MEASUREMENTS_POLL_INTERVAL_MS: 1000,
    REQUEST_TIMEOUT_MS: 3000
});
```

* `CUS_BASE_URL` is the HTTP address used for CUS API requests.
* `STATE_POLL_INTERVAL_MS` is the delay between state updates, in milliseconds.
* `MEASUREMENTS_POLL_INTERVAL_MS` is the delay between measurement updates, in milliseconds.
* `REQUEST_TIMEOUT_MS` is the maximum time allowed for a request, in milliseconds.

The state and measurement polling loops run separately. Each loop waits for its current request to finish before scheduling the next one.

## CUS API

The DBS uses the following endpoints:

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

Possible modes are `AUTOMATIC`, `MANUAL` and `UNCONNECTED`.

`waterLevel` may be `null` when no valid measurement is available.

Example measurements response:

```json
[
  { "timestamp": 1759062000000, "value": 40 },
  { "timestamp": 1759062001000, "value": 44 },
  { "timestamp": 1759062002000, "value": null }
]
```

The DBS also accepts an object containing a `measurements` array. Null measurement values are shown as gaps in the chart.

The dashboard sends these JSON bodies for operator requests:

```json
{ "mode": "MANUAL" }
```

```json
{ "opening": 50 }
```

## Dashboard behaviour

The CUS is the source of truth for the system state.

The DBS sends operator requests, but it updates the displayed mode and valve opening only after reading the state again from the CUS.

The mode button switches between `AUTOMATIC` and `MANUAL`. The valve slider is enabled only in `MANUAL` mode. A new valve value is sent when the slider change is committed.

If the CUS state cannot be read, the dashboard shows `NOT AVAILABLE`, disables the controls and keeps the last known values visible. It also shows the time of the last successful state update.

The chart has its own update loop. If the measurement request fails, the chart shows an error message but the connection indicator is not changed by that error alone.

## Chart.js

Chart.js is already stored in the project at:

```text
lib/chart.umd.min.js
```

It is loaded by `index.html` using:

```html
<script src="lib/chart.umd.min.js"></script>
```

No CDN or extra download is needed to display the chart.
