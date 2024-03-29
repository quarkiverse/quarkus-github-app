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
public class ParseErrorStrategyCliTest {

    @Test
    void testParseErrorStrategy() throws IOException {
        when().payloadFromClasspath("/issue-comment-parse-error-strategy-invalid.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554))
                            .comment("test parse error message: @parse-error-strategy invalid-command");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.CONFUSED);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
