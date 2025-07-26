package io.quarkiverse.githubapp.command.airline;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.command.airline.runtime.AbstractCommandDispatcher.CommandExecutionContext;

/**
 * This allows to configure a specific error handler for command execution errors.
 * <p>
 * It is recommended to make the implementations singleton-scoped beans.
 * <p>
 * The execution error handler can be configured at the {@link CliOptions} level or more specifically at the
 * {@link CommandOptions} level.
 */
public interface ExecutionErrorHandler {

    void handleExecutionError(GHEventPayload.IssueComment issueCommentPayload, ExecutionErrorContext executionErrorContext);

    public record ExecutionErrorContext(CommandExecutionContext<?> commandExecutionContext, Exception exception) {
    }
}
