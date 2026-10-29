/**
 * Creates and updates the water level chart.
 * Chart.js is loaded by index.html.
 * @module chart
 */
let chart = null;

/**
 * Converts a measurement timestamp to local time.
 * @param {number|string|null|undefined} timestamp - Measurement time.
 * @returns {string} Local time, or an empty string if no time is given.
 */
function formatTimestamp(timestamp) {
    if (timestamp === null || timestamp === undefined) {
        return "";
    }

    if (typeof timestamp === "number") {
        return new Date(timestamp).toLocaleTimeString();
    }

    const parsed = new Date(timestamp);
    if (!Number.isNaN(parsed.getTime())) {
        return parsed.toLocaleTimeString();
    }

    return String(timestamp);
}

/**
 * Creates an empty line chart for the water level.
 * The Y axis goes from 0 to 100 percent.
 *
 * @param {HTMLCanvasElement} canvas - Canvas used to draw the chart.
 * @throws {Error} If Chart.js is not loaded.
 */
export function initWaterLevelChart(canvas) {
    if (!window.Chart) {
        throw new Error("Chart.js is not available");
    }

    chart = new window.Chart(canvas, {
        type: "line",
        data: {
            labels: [],
            datasets: [{
                label: "Water level",
                data: [],
                tension: 0.2,
                spanGaps: false,
                pointRadius: 3,
                pointHoverRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: false,
            interaction: {
                intersect: false,
                mode: "index"
            },
            scales: {
                y: {
                    min: 0,
                    max: 100,
                    title: {
                        display: true,
                        text: "Water level (%)"
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: "Time"
                    }
                }
            }
        }
    });
}

/**
 * Shows the latest measurements on the chart.
 * Null water levels are shown as gaps.
 *
 * @param {Array<Object>} measurements - Items with timestamp and value fields.
 */
export function updateWaterLevelChart(measurements) {
    if (!chart) {
        return;
    }

    chart.data.labels = measurements.map(item => formatTimestamp(item.timestamp));
    chart.data.datasets[0].data = measurements.map(item => item.value);
    chart.update("none");
}
