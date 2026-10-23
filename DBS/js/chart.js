let chart = null;

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

// Crea il grafico iniziale
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

// Aggiorna i valori
export function updateWaterLevelChart(measurements) {
    if (!chart) {
        return;
    }

    chart.data.labels = measurements.map(item => formatTimestamp(item.timestamp));
    chart.data.datasets[0].data = measurements.map(item => item.value);
    chart.update("none");
}
