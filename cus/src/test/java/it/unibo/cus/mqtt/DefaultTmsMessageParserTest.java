package it.unibo.cus.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DefaultTmsMessageParserTest {

    private final DefaultTmsMessageParser parser = new DefaultTmsMessageParser();

    @Test
    void parsesValidTmsMeasurementUsingPercentage() {
        final Double value = parser.parse(
            "{\"status\":\"OK\",\"levelM\":0.420,\"percentage\":42.0}"
        );

        assertEquals(42.0, value);
    }

    @Test
    void convertsNoEchoToNull() {
        final Double value = parser.parse("{\"status\":\"NO_ECHO\"}");

        assertNull(value);
    }

    @Test
    void rejectsOkPayloadWithoutPercentage() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse("{\"status\":\"OK\",\"levelM\":0.420}")
        );
    }

    @Test
    void rejectsPercentageOutsideExpectedRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse(
                "{\"status\":\"OK\",\"levelM\":1.200,\"percentage\":120.0}"
            )
        );
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse("{\"status\":\"SENSOR_ERROR\"}")
        );
    }

    @Test
    void rejectsMalformedPayload() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse("not-json")
        );
    }

    @Test
    void acceptsPercentageBoundaries() {
        assertEquals(
            0.0,
            parser.parse("{\"status\":\"OK\",\"percentage\":0}")
        );

        assertEquals(
            100.0,
            parser.parse("{\"status\":\"OK\",\"percentage\":100}")
        );
    }

    @Test
    void rejectsNonNumericPercentage() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse(
                "{\"status\":\"OK\",\"percentage\":\"42.0\"}"
            )
        );
    }

    @Test
    void rejectsNonStringStatus() {
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse("{\"status\":123}")
        );
    }
}
