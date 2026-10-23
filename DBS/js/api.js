import { CONFIG } from "./config.js";

export class ApiError extends Error {
    constructor(message, { status = null, network = false } = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.network = network;
    }
}

async function fetchJson(path, options = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), CONFIG.REQUEST_TIMEOUT_MS);

    try {
        const response = await fetch(`${CONFIG.CUS_BASE_URL}${path}`, {
            ...options,
            signal: controller.signal,
            headers: {
                "Accept": "application/json",
                ...(options.body ? { "Content-Type": "application/json" } : {}),
                ...(options.headers || {})
            }
        });

        if (!response.ok) {
            let detail = `HTTP ${response.status}`;
            try {
                const body = await response.json();
                detail = body.message || body.error || detail;
            } catch (_) {
                // The response body is optional for an error.
            }
            throw new ApiError(detail, { status: response.status });
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        if (!contentType.includes("application/json")) {
            return null;
        }

        return await response.json();
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }

        if (error.name === "AbortError") {
            throw new ApiError("CUS request timed out", { network: true });
        }

        throw new ApiError("CUS is not reachable", { network: true });
    } finally {
        clearTimeout(timeoutId);
    }
}

export function getState() {
    return fetchJson("/api/state");
}

export function getMeasurements() {
    return fetchJson("/api/measurements");
}

export function setMode(mode) {
    return fetchJson("/api/mode", {
        method: "POST",
        body: JSON.stringify({ mode })
    });
}

export function setValveOpening(opening) {
    return fetchJson("/api/valve", {
        method: "POST",
        body: JSON.stringify({ opening })
    });
}
