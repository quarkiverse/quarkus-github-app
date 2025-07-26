package io.quarkiverse.githubapp.command.airline;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.parser.ParseResult;

import io.quarkiverse.githubapp.command.airline.runtime.CliConfig;

/**
 * This allows to configure a specific error handler for parse errors.
 * <p>
 * It is recommended to make the implementations singleton-scoped beans.
 * <p>
 * The execution error handler can be configured at the {@link CliOptions} level.
 */
public interface ParseErrorHandler {

    void handleParseError(GHEventPayload.IssueComment issueCommentPayload, ParseErrorContext parseErrorContext);

    public record ParseErrorContext(CliConfig cliConfig, Cli<?> cli, String command, ParseResult<?> parseResult, String error) {
    }
}
