package command.airline;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHEventPayload.IssueComment;

import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler;

// tag::execution-error-handler[]
@Singleton
public class CustomExecutionErrorHandler implements ExecutionErrorHandler {

    @Override
    public void handleExecutionError(IssueComment issueCommentPayload, ExecutionErrorContext executionErrorContext) {
        // your custom logic here
    }
}
// end::execution-error-handler[]
