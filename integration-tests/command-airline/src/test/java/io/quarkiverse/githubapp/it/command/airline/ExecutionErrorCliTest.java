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
public class ExecutionErrorCliTest {

    @Test
    void testExecutionError() throws IOException {
        when().payloadFromClasspath("/issue-comment-execution-error.json")
                .event(GHEvent.ISSUE_COMMENT, true)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.MINUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
