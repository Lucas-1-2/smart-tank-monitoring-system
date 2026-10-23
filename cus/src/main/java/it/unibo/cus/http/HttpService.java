package it.unibo.cus.http;

import it.unibo.cus.Config;
import it.unibo.cus.core.CommandResult;
import it.unibo.cus.core.SystemController;
import it.unibo.cus.model.Measurement;
import it.unibo.cus.model.OperatingMode;
import it.unibo.cus.model.SystemSnapshot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** HTTP REST API and static DBS server, based on Vert.x Web. */
public final class HttpService extends AbstractVerticle {

    private final SystemController controller;

    public HttpService(final SystemController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    @Override
    public void start(final Promise<Void> startPromise) {
        final Router router = Router.router(vertx);

        /* BodyHandler is required for POST JSON bodies, as in the course examples. */
        router.post("/api/*").handler(BodyHandler.create());

        router.get("/api/state").handler(this::handleGetState);
        router.get("/api/measurements").handler(this::handleGetMeasurements);
        router.post("/api/mode").handler(this::handleSetMode);
        router.post("/api/valve").handler(this::handleSetValve);

        /*
         * DBS is served by the same HTTP origin as the REST API.
         * Therefore the frontend can use relative /api/... URLs and no CORS is needed.
         */
        final String dbsRoot = Path.of(Config.DBS_DIRECTORY)
            .toAbsolutePath()
            .normalize()
            .toString();

        final StaticHandler staticHandler = StaticHandler
            .create(FileSystemAccess.ROOT, dbsRoot)
            .setCachingEnabled(false)
            .setIndexPage("index.html");

        router.route("/*").handler(staticHandler);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(Config.HTTP_PORT, Config.HTTP_HOST, result -> {
                if (result.succeeded()) {
                    log("Service ready on http://localhost:" + Config.HTTP_PORT);
                    log("Listening on " + Config.HTTP_HOST + ":" + Config.HTTP_PORT);
                    log("DBS directory: " + dbsRoot);
                    startPromise.complete();
                } else {
                    startPromise.fail(result.cause());
                }
            });
    }

    private void handleGetState(final RoutingContext context) {
        sendJson(context.response(), stateToJson(controller.getSnapshot()));
    }

    private void handleGetMeasurements(final RoutingContext context) {
        final List<Measurement> measurements = controller.getMeasurementsSnapshot();
        final JsonArray array = new JsonArray();

        for (final Measurement measurement : measurements) {
            final JsonObject json = new JsonObject()
                .put("timestamp", measurement.timestamp())
                .put("value", measurement.value());
            array.add(json);
        }

        context.response()
            .putHeader("content-type", "application/json")
            .end(array.encode());
    }

    private JsonObject readJsonBody(final RoutingContext context) {
        final JsonObject body;

        try {
            body = context.getBodyAsJson();
        } catch (final DecodeException exception) {
            sendError(context.response(), 400, "Malformed JSON body");
            return null;
        }

        if (body == null) {
            sendError(context.response(), 400, "JSON object body is required");
        }

        return body;
    }

    private void handleSetMode(final RoutingContext context) {
        final JsonObject body = readJsonBody(context);

        if (body == null) {
            return;
        }

        final Object rawMode = body.getValue("mode");

        if (!(rawMode instanceof String mode)) {
            sendError(
                context.response(),
                400,
                "Field 'mode' must be a string"
            );
            return;
        }

        final OperatingMode requestedMode;

        try {
            requestedMode = OperatingMode.valueOf(
                mode.trim().toUpperCase(Locale.ROOT)
            );
        } catch (final IllegalArgumentException exception) {
            sendError(
                context.response(),
                400,
                "Mode must be AUTOMATIC or MANUAL"
            );
            return;
        }

        final CommandResult result =
            controller.requestMode(requestedMode);

        if (!result.accepted()) {
            sendError(context.response(), 409, result.message());
            return;
        }

        context.response().setStatusCode(204).end();
    }

    private void handleSetValve(final RoutingContext context) {
        final JsonObject body = readJsonBody(context);

        if (body == null) {
            return;
        }

        final Object rawOpening = body.getValue("opening");
        if (!(rawOpening instanceof Number number)) {
            sendError(context.response(), 400, "Field 'opening' must be a number");
            return;
        }

        final double numericOpening = number.doubleValue();
        if (!Double.isFinite(numericOpening)
            || numericOpening < 0.0
            || numericOpening > 100.0
            || numericOpening != Math.rint(numericOpening)) {

            sendError(context.response(), 400, "Opening must be an integer in [0,100]");
            return;
        }

        final CommandResult result;
        try {
            result = controller.requestValveOpening((int) numericOpening);
        } catch (final IllegalArgumentException exception) {
            sendError(context.response(), 400, exception.getMessage());
            return;
        }

        if (!result.accepted()) {
            sendError(context.response(), 409, result.message());
            return;
        }

        context.response().setStatusCode(204).end();
    }

    private JsonObject stateToJson(final SystemSnapshot snapshot) {
        return new JsonObject()
            .put("mode", snapshot.state().name())
            .put("valveOpening", snapshot.valveOpening())
            .put("waterLevel", snapshot.waterLevel());
    }

    private void sendJson(final HttpServerResponse response, final JsonObject body) {
        response
            .putHeader("content-type", "application/json")
            .end(body.encode());
    }

    private void sendError(
        final HttpServerResponse response,
        final int statusCode,
        final String message
    ) {
        response
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(new JsonObject().put("message", message).encode());
    }

    private void log(final String message) {
        System.out.println("[HTTP] " + message);
    }
}
