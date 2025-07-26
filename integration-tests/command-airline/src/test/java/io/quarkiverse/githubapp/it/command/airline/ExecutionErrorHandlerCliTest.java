package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class ExecutionErrorHandlerCliTest {

    @Test
    void testExecutionErrorStrategy() throws IOException {
        when().payloadFromClasspath("/issue-comment-execution-error-handler.json")
                .event(GHEvent.ISSUE_COMMENT, true)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("My custom error message.");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.MINUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
