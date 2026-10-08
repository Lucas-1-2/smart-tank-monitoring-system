package it.unibo.cus.core;

import it.unibo.cus.model.SystemSnapshot;
import java.util.Objects;

/** Result of a command coming from WCS or DBS. */
public record CommandResult(
    boolean accepted,
    boolean changed,
    String message,
    SystemSnapshot snapshot
) {
    public CommandResult {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(snapshot, "snapshot");
    }

    public static CommandResult accepted(
        final boolean changed,
        final String message,
        final SystemSnapshot snapshot
    ) {
        return new CommandResult(true, changed, message, snapshot);
    }

    public static CommandResult rejected(
        final String message,
        final SystemSnapshot snapshot
    ) {
        return new CommandResult(false, false, message, snapshot);
    }
}
