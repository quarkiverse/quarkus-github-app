package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.mockito.ArgumentMatchers.any;
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
public class ReactiveStrategyOverrideCliTest {

    @Test
    void testNoOverride() throws IOException {
        when().payloadFromClasspath("/issue-comment-reaction-strategy-override-no-override-success.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554))
                            .comment("hello from @reaction-strategy-override no-override");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        when().payloadFromClasspath("/issue-comment-reaction-strategy-override-no-override-error.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554)).comment(any());
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testOverride() throws IOException {
        when().payloadFromClasspath("/issue-comment-reaction-strategy-override-success.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("hello from @reaction-strategy-override override");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.PLUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        when().payloadFromClasspath("/issue-comment-reaction-strategy-override-error.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554)).comment(any());
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.CONFUSED);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
