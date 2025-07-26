package io.quarkiverse.githubapp.command.airline.runtime;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler;

@Singleton
public class DefaultExecutionErrorHandler implements ExecutionErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(DefaultExecutionErrorHandler.class);

    @Override
    public void handleExecutionError(GHEventPayload.IssueComment issueCommentPayload,
            ExecutionErrorContext executionErrorContext) {
        CommandConfig commandConfig = executionErrorContext.commandExecutionContext().getCommandConfig();

        if (!commandConfig.getExecutionErrorStrategy().addMessage()) {
            return;
        }

        String commandLine = executionErrorContext.commandExecutionContext().getCommandLine();
        try {
            issueCommentPayload.getIssue()
                    .comment(String.format(commandConfig.getExecutionErrorMessage(), commandLine));
        } catch (Exception e) {
            LOGGER.warn("Error trying to add command execution error comment for command `" + commandLine + "` in "
                    + issueCommentPayload.getRepository().getFullName() + "#" + issueCommentPayload.getIssue().getNumber(), e);
        }
    }
}
