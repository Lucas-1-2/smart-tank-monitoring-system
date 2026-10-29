/**
 * Sends HTTP requests to the CUS.
 * @module api
 */
import { CONFIG } from "./config.js";

/**
 * Error used when a CUS request fails.
 */
export class ApiError extends Error {
    /**
     * Creates an error with HTTP or network details.
     * @param {string} message - Error message.
     * @param {Object} [options] - Extra error details.
     * @param {number|null} [options.status=null] - HTTP status, when available.
     * @param {boolean} [options.network=false] - True for a network error or timeout.
     */
    constructor(message, { status = null, network = false } = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.network = network;
    }
}

/**
 * Sends a request and reads the JSON response.
 * The request has a timeout. Failed requests throw an ApiError.
 *
 * @param {string} path - Path of the CUS API endpoint.
 * @param {Object} [options] - Options passed to fetch().
 * @returns {Promise<*>} JSON data, or null if the response has no JSON body.
 * @throws {ApiError} If the request fails or times out.
 */
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

/**
 * Gets the current system state from the CUS.
 * @returns {Promise<Object|null>} Current state data.
 */
export function getState() {
    return fetchJson("/api/state");
}

/**
 * Gets the water level history from the CUS.
 * @returns {Promise<Array|Object|null>} Measurements data.
 */
export function getMeasurements() {
    return fetchJson("/api/measurements");
}

/**
 * Requests a new operating mode.
 * @param {string} mode - New mode (AUTOMATIC or MANUAL).
 * @returns {Promise<*>} CUS response.
 */
export function setMode(mode) {
    return fetchJson("/api/mode", {
        method: "POST",
        body: JSON.stringify({ mode })
    });
}

/**
 * Requests a new valve opening.
 * @param {number} opening - Opening percentage, from 0 to 100.
 * @returns {Promise<*>} CUS response.
 */
export function setValveOpening(opening) {
    return fetchJson("/api/valve", {
        method: "POST",
        body: JSON.stringify({ opening })
    });
}
