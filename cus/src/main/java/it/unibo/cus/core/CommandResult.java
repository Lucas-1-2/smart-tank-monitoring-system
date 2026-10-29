package it.unibo.cus.core;

import it.unibo.cus.model.SystemSnapshot;
import java.util.Objects;

/**
 * Represents the result of a command received from WCS or DBS.
 *
 * A command can be accepted without changing the system state.
 * The result also contains a message and a snapshot of the system.
 *
 * @param accepted true if the command was accepted
 * @param changed true if the command changed the system state
 * @param message a message describing the result
 * @param snapshot the system state after the command
 */
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

    /**
     * Creates a result for an accepted command.
     *
     * @param changed true if the system state changed
     * @param message a message describing the result
     * @param snapshot the system state after the command
     * @return the command result
     */
    public static CommandResult accepted(
        final boolean changed,
        final String message,
        final SystemSnapshot snapshot
    ) {
        return new CommandResult(true, changed, message, snapshot);
    }

    /**
     * Creates a result for a rejected command.
     *
     * @param message a message explaining the rejection
     * @param snapshot the current system state
     * @return the command result
     */
    public static CommandResult rejected(
        final String message,
        final SystemSnapshot snapshot
    ) {
        return new CommandResult(false, false, message, snapshot);
    }
}