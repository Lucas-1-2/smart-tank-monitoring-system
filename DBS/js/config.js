/**
 * Contains the main DBS settings.
 * The CUS address is used for every API request.
 * Polling intervals and request timeout are in milliseconds.
 *
 * @module config
 */
/**
 * Fixed settings for HTTP requests and polling.
 * @readonly
 * @type {Object}
 */
export const CONFIG = Object.freeze({
    CUS_BASE_URL: "http://localhost:8080",
    STATE_POLL_INTERVAL_MS: 1000,
    MEASUREMENTS_POLL_INTERVAL_MS: 1000,
    REQUEST_TIMEOUT_MS: 3000
});
