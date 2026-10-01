import { CONFIG } from "./config.js";
import {
    ApiError,
    getMeasurements,
    getState,
    setMode,
    setValveOpening
} from "./api.js";
import { initWaterLevelChart, updateWaterLevelChart } from "./chart.js";

const SystemMode = Object.freeze({
    AUTOMATIC: "AUTOMATIC",
    MANUAL: "MANUAL",
    UNCONNECTED: "UNCONNECTED"
});

const dashboardState = {
    connectionAvailable: false,
    mode: null,
    valveOpening: null,
    waterLevel: null,
    measurements: [],
    lastSuccessfulUpdate: null,
    modeRequestPending: false,
    valveRequestPending: false,
    sliderEditing: false
};

const elements = {};

function cacheElements() {
    elements.connectionIndicator = document.getElementById("connection-indicator");
    elements.connectionLabel = document.getElementById("connection-label");
    elements.lastUpdate = document.getElementById("last-update");
    elements.systemMode = document.getElementById("system-mode");
    elements.waterLevel = document.getElementById("water-level");
    elements.valveOpening = document.getElementById("valve-opening");
    elements.modeButton = document.getElementById("mode-button");
    elements.modeHelp = document.getElementById("mode-help");
    elements.valveSlider = document.getElementById("valve-slider");
    elements.selectedValveValue = document.getElementById("selected-valve-value");
    elements.valveHelp = document.getElementById("valve-help");
    elements.requestMessage = document.getElementById("request-message");
    elements.chartStatus = document.getElementById("chart-status");
    elements.chartCanvas = document.getElementById("water-level-chart");
}

function isValidPercentage(value) {
    return Number.isFinite(value) && value >= 0 && value <= 100;
}

function normalizeMode(mode) {
    const normalized = String(mode || "").toUpperCase();
    return Object.values(SystemMode).includes(normalized) ? normalized : null;
}

function applyStatePayload(payload) {
    if (!payload || typeof payload !== "object") {
        throw new Error("Invalid state payload");
    }

    const mode = normalizeMode(payload.mode);
    const valveOpening = Number(payload.valveOpening);
    const waterLevel = payload.waterLevel === null ? null : Number(payload.waterLevel);

    if (!mode || !isValidPercentage(valveOpening)) {
        throw new Error("Invalid state values");
    }

    if (waterLevel !== null && !isValidPercentage(waterLevel)) {
        throw new Error("Invalid water level");
    }

    dashboardState.mode = mode;
    dashboardState.valveOpening = valveOpening;
    dashboardState.waterLevel = waterLevel;
    dashboardState.connectionAvailable = true;
    dashboardState.lastSuccessfulUpdate = new Date();
}

function normalizeMeasurements(payload) {
    const items = Array.isArray(payload) ? payload : payload?.measurements;
    if (!Array.isArray(items)) {
        throw new Error("Invalid measurements payload");
    }

    return items.map(item => {
        const value = item.value === null ? null : Number(item.value);
        if (value !== null && !isValidPercentage(value)) {
            throw new Error("Invalid measurement value");
        }

        return {
            timestamp: item.timestamp,
            value
        };
    });
}

function formatPercentage(value) {
    return isValidPercentage(value) ? `${Math.round(value)}%` : "--";
}

function formatLastUpdate(date) {
    return date ? date.toLocaleTimeString() : "--";
}

function setRequestMessage(message = "", type = "") {
    elements.requestMessage.textContent = message;
    elements.requestMessage.className = `request-message${type ? ` ${type}` : ""}`;
}

function renderConnection() {
    const available = dashboardState.connectionAvailable;
    elements.connectionIndicator.classList.toggle("available", available);
    elements.connectionIndicator.classList.toggle("unavailable", !available);
    elements.connectionLabel.textContent = available ? "CUS available" : "CUS not available";
    elements.lastUpdate.textContent = `Last update: ${formatLastUpdate(dashboardState.lastSuccessfulUpdate)}`;
}

function renderSummary() {
    elements.systemMode.textContent = dashboardState.connectionAvailable
        ? dashboardState.mode
        : "NOT AVAILABLE";

    elements.valveOpening.textContent = formatPercentage(dashboardState.valveOpening);
    elements.waterLevel.textContent = dashboardState.waterLevel === null
        ? "--"
        : formatPercentage(dashboardState.waterLevel);
}

function renderModeControl() {
    const mode = dashboardState.mode;
    const enabled = dashboardState.connectionAvailable
        && !dashboardState.modeRequestPending
        && (mode === SystemMode.AUTOMATIC || mode === SystemMode.MANUAL);

    elements.modeButton.disabled = !enabled;

    if (mode === SystemMode.AUTOMATIC) {
        elements.modeButton.textContent = "Switch to MANUAL";
        elements.modeHelp.textContent = "Press to request MANUAL mode.";
    } else if (mode === SystemMode.MANUAL) {
        elements.modeButton.textContent = "Switch to AUTOMATIC";
        elements.modeHelp.textContent = "Press to request AUTOMATIC mode.";
    } else if (mode === SystemMode.UNCONNECTED) {
        elements.modeButton.textContent = "Switch mode";
        elements.modeHelp.textContent = "Controls are disabled while the system is UNCONNECTED.";
    } else {
        elements.modeButton.textContent = "Switch mode";
        elements.modeHelp.textContent = "Controls are disabled while CUS is not available.";
    }
}

function renderValveControl() {
    const enabled = dashboardState.connectionAvailable
        && dashboardState.mode === SystemMode.MANUAL
        && !dashboardState.valveRequestPending;

    elements.valveSlider.disabled = !enabled;

    if (!dashboardState.sliderEditing && isValidPercentage(dashboardState.valveOpening)) {
        elements.valveSlider.value = Math.round(dashboardState.valveOpening);
        elements.selectedValveValue.textContent = `Selected: ${Math.round(dashboardState.valveOpening)}%`;
    }

    if (!dashboardState.connectionAvailable) {
        elements.valveHelp.textContent = "Controls are disabled while CUS is not available.";
    } else if (dashboardState.mode === SystemMode.UNCONNECTED) {
        elements.valveHelp.textContent = "Controls are disabled while the system is UNCONNECTED.";
    } else if (dashboardState.mode !== SystemMode.MANUAL) {
        elements.valveHelp.textContent = "Available only in MANUAL mode.";
    } else if (dashboardState.valveRequestPending) {
        elements.valveHelp.textContent = "Sending valve request...";
    } else {
        elements.valveHelp.textContent = "The new value is sent when the slider is released.";
    }
}

function renderDashboard() {
    renderConnection();
    renderSummary();
    renderModeControl();
    renderValveControl();
}

function markCusUnavailable() {
    dashboardState.connectionAvailable = false;
    renderDashboard();
}

async function refreshState() {
    try {
        const payload = await getState();
        applyStatePayload(payload);
        renderDashboard();
        return true;
    } catch (error) {
        console.error("State refresh failed:", error);
        markCusUnavailable();
        return false;
    }
}

async function refreshMeasurements() {
    try {
        const payload = await getMeasurements();
        dashboardState.measurements = normalizeMeasurements(payload);
        updateWaterLevelChart(dashboardState.measurements);
        elements.chartStatus.textContent = `${dashboardState.measurements.length} measurements`;
        elements.chartStatus.className = "chart-status";
        return true;
    } catch (error) {
        console.error("Measurements refresh failed:", error);
        elements.chartStatus.textContent = "Measurements not updated";
        elements.chartStatus.className = "chart-status error";
        return false;
    }
}

async function statePollingLoop() {
    await refreshState();
    setTimeout(statePollingLoop, CONFIG.STATE_POLL_INTERVAL_MS);
}

async function measurementsPollingLoop() {
    await refreshMeasurements();
    setTimeout(measurementsPollingLoop, CONFIG.MEASUREMENTS_POLL_INTERVAL_MS);
}

async function handleModeClick() {
    if (dashboardState.modeRequestPending || !dashboardState.connectionAvailable) {
        return;
    }

    let targetMode = null;
    if (dashboardState.mode === SystemMode.AUTOMATIC) {
        targetMode = SystemMode.MANUAL;
    } else if (dashboardState.mode === SystemMode.MANUAL) {
        targetMode = SystemMode.AUTOMATIC;
    }

    if (!targetMode) {
        return;
    }

    dashboardState.modeRequestPending = true;
    setRequestMessage(`Requesting ${targetMode} mode...`);
    renderDashboard();

    try {
        await setMode(targetMode);
        setRequestMessage("Mode request received by CUS.", "success");
        await refreshState();
    } catch (error) {
        if (error instanceof ApiError && !error.network) {
            setRequestMessage(`Mode request rejected: ${error.message}`, "error");
            await refreshState();
        } else {
            setRequestMessage("CUS is not reachable.", "error");
            markCusUnavailable();
        }
    } finally {
        dashboardState.modeRequestPending = false;
        renderDashboard();
    }
}

function handleSliderInput() {
    dashboardState.sliderEditing = true;
    elements.selectedValveValue.textContent = `Selected: ${elements.valveSlider.value}%`;
}

async function handleSliderChange() {
    if (dashboardState.valveRequestPending
        || !dashboardState.connectionAvailable
        || dashboardState.mode !== SystemMode.MANUAL) {
        dashboardState.sliderEditing = false;
        renderValveControl();
        return;
    }

    const requestedOpening = Number(elements.valveSlider.value);
    dashboardState.valveRequestPending = true;
    dashboardState.sliderEditing = false;
    setRequestMessage(`Requesting valve opening ${requestedOpening}%...`);
    renderDashboard();

    try {
        await setValveOpening(requestedOpening);
        setRequestMessage("Valve request received by CUS.", "success");
        await refreshState();
    } catch (error) {
        if (error instanceof ApiError && !error.network) {
            setRequestMessage(`Valve request rejected: ${error.message}`, "error");
            await refreshState();
        } else {
            setRequestMessage("CUS is not reachable.", "error");
            markCusUnavailable();
        }
    } finally {
        dashboardState.valveRequestPending = false;
        renderDashboard();
    }
}

function registerEventHandlers() {
    elements.modeButton.addEventListener("click", handleModeClick);
    elements.valveSlider.addEventListener("input", handleSliderInput);
    elements.valveSlider.addEventListener("change", handleSliderChange);
}

function startChart() {
    try {
        initWaterLevelChart(elements.chartCanvas);
    } catch (error) {
        console.error("Chart initialization failed:", error);
        elements.chartStatus.textContent = "Chart.js not available";
        elements.chartStatus.className = "chart-status error";
    }
}

function start() {
    cacheElements();
    registerEventHandlers();
    startChart();
    renderDashboard();
    statePollingLoop();
    measurementsPollingLoop();
}

window.addEventListener("DOMContentLoaded", start);
