package it.unibo.cus.http.api;

import io.vertx.core.Promise;
import io.vertx.core.Verticle;

/**
 * Provides the HTTP server for the DBS and the CUS REST API.
 *
 * The API lets the DBS read the current state and measurement history.
 * It also accepts requests to change the mode or the manual valve opening.
 * The DBS web files are served by the same HTTP server.
 *
 * This service is a Vert.x verticle. Vert.x manages its lifecycle.
 */
public interface HttpService extends Verticle {

    /**
     * Starts the HTTP server and registers the REST routes.
     * The promise reports whether the server started correctly.
     *
     * @param startPromise the promise completed when the server is ready
     */
    @Override
    void start(Promise<Void> startPromise);
}
