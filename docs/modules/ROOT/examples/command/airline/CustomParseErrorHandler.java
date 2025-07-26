package command.airline;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHEventPayload.IssueComment;

import io.quarkiverse.githubapp.command.airline.ParseErrorHandler;

// tag::parse-error-handler[]
@Singleton
public class CustomParseErrorHandler implements ParseErrorHandler {

    @Override
    public void handleParseError(IssueComment issueCommentPayload, ParseErrorContext parseErrorContext) {
        // your custom logic here
    }
}
// end::parse-error-handler[]
