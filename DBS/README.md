# DBS - Dashboard Subsystem

Frontend web for Assignment 03 - Tank Monitoring System.

## Technology

- HTML
- CSS
- Vanilla JavaScript
- Fetch API
- Chart.js
- XAMPP / Apache as local web server

No frontend framework is required.

## Run with XAMPP

1. Copy the `dbs` folder into the XAMPP `htdocs` folder, for example:

   `C:\xampp\htdocs\dbs`

2. Start Apache from the XAMPP control panel.
3. Start the CUS backend on the URL configured in `js/config.js`.
4. Open:

   `http://localhost/dbs/`

## Configuration

Edit `js/config.js`:

```js
export const CONFIG = Object.freeze({
    CUS_BASE_URL: "http://localhost:8080",
    STATE_POLL_INTERVAL_MS: 1000,
    MEASUREMENTS_POLL_INTERVAL_MS: 1000,
    REQUEST_TIMEOUT_MS: 3000
});
```

`STATE_POLL_INTERVAL_MS` and `MEASUREMENTS_POLL_INTERVAL_MS` are intentionally independent so they can be tuned separately.

## Expected CUS HTTP API

### GET /api/state

Expected response:

```json
{
  "mode": "AUTOMATIC",
  "valveOpening": 0,
  "waterLevel": 42
}
```

Allowed `mode` values:

- `AUTOMATIC`
- `MANUAL`
- `UNCONNECTED`

`valveOpening` and `waterLevel` are percentages in the range 0-100. `waterLevel` may be `null` when the current measurement is not available.

### GET /api/measurements

Expected response:

```json
[
  { "timestamp": 1759062000000, "value": 40 },
  { "timestamp": 1759062001000, "value": 44 },
  { "timestamp": 1759062002000, "value": null },
  { "timestamp": 1759062003000, "value": 47 }
]
```

A `null` measurement is kept as a gap in the graph.

The CUS is expected to retain and return the last N measurements.

### POST /api/mode

Request body:

```json
{
  "mode": "MANUAL"
}
```

or:

```json
{
  "mode": "AUTOMATIC"
}
```

`UNCONNECTED` cannot be requested by the operator.

### POST /api/valve

Request body:

```json
{
  "opening": 65
}
```

The CUS decides whether the request can be applied. In particular, manual valve requests should only be applied while the global system mode is `MANUAL`.

## Source of truth

The CUS owns the global system state.

The DBS never changes the displayed global state just because the user presses a button or moves the slider. It sends the request to the CUS and then refreshes `/api/state`. The confirmed CUS state is what updates the dashboard.

## Mode button

The HTML `click` event is used. A click represents activation of the button and normally occurs after press and release with a pointer. It also correctly supports keyboard activation.

## Valve slider

- `input` updates only the temporary selected value shown next to the slider.
- `change` sends the new opening value to the CUS. For a range input this corresponds to committing the new value, normally when the slider is released.
- The official valve opening shown in the summary is updated only from the next confirmed CUS state.

## Polling and NOT AVAILABLE

Two independent polling loops are used:

- `/api/state`
- `/api/measurements`

They have separate configurable intervals.

If `/api/state` cannot be obtained, the dashboard displays `NOT AVAILABLE`, keeps the last known values visible and disables operator controls. `Last update` shows the time of the last successful state response.

If only `/api/measurements` fails while `/api/state` still works, the CUS is considered reachable and the previous graph is kept with a `Measurements not updated` warning.

## CORS

When DBS is served by XAMPP at `http://localhost/dbs/` and CUS is running at `http://localhost:8080`, the browser sees two different origins because the ports are different.

The CUS must therefore allow CORS requests from the DBS origin, normally:

`http://localhost`

This configuration belongs to the CUS backend.

## Chart.js

The current `index.html` loads Chart.js from jsDelivr CDN. This keeps the submitted DBS small and immediately usable when an Internet connection is available.

For a completely offline demo, download the Chart.js UMD build and place it in `lib/chart.umd.min.js`, then replace the CDN script in `index.html` with:

```html
<script src="lib/chart.umd.min.js"></script>
```
