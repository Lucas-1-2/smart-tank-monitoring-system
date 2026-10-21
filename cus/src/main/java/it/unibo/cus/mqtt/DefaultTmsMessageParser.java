package it.unibo.cus.mqtt;

import io.vertx.core.json.JsonObject;

/**
 * Parser for the MQTT payload produced by the Tank Monitoring Subsystem.
 *
 * Supported payloads:
 * - valid measurement:
 *   {"status":"OK","levelM":0.420,"percentage":42.0}
 * - invalid/unavailable sonar measurement:
 *   {"status":"NO_ECHO"}
 *
 * The CUS works with the percentage value only. NO_ECHO is normalized to null;
 * the controller stores it in the measurement history but does not reset T2.
 */
public final class DefaultTmsMessageParser implements TmsMessageParser {

    private static final String STATUS_OK = "OK";
    private static final String STATUS_NO_ECHO = "NO_ECHO";

    @Override
    public Double parse(final String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("MQTT payload cannot be null");
        }

        final String normalized = payload.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("MQTT payload cannot be empty");
        }

        final JsonObject json;
        try {
            json = new JsonObject(normalized);
        } catch (final RuntimeException exception) {
            throw new IllegalArgumentException("Invalid JSON TMS payload", exception);
        }

        final Object rawStatus = json.getValue("status");
        if (!(rawStatus instanceof String status) || status.isBlank()) {
            throw new IllegalArgumentException(
                "TMS JSON field 'status' must be a non-empty string"
            );
        }

        return switch (status) {
            case STATUS_OK -> parseValidMeasurement(json);
            case STATUS_NO_ECHO -> null;
            default -> throw new IllegalArgumentException(
                "Unsupported TMS status: " + status
            );
        };
    }

    private Double parseValidMeasurement(final JsonObject json) {
        final Object rawPercentage = json.getValue("percentage");
        if (!(rawPercentage instanceof Number number)) {
            throw new IllegalArgumentException(
                "TMS JSON field 'percentage' must be numeric when status is OK"
            );
        }

        final double percentage = number.doubleValue();
        if (!Double.isFinite(percentage) || percentage < 0.0 || percentage > 100.0) {
            throw new IllegalArgumentException(
                "TMS percentage must be in [0,100]"
            );
        }

        return percentage;
    }
}
