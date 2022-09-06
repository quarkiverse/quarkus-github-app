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
public class ReactiveStrategyCliTest {

    @Test
    void testAll() throws IOException {
        when().payloadFromClasspath("/issue-comment-reaction-strategy-all-success.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("hello from @reaction-strategy all");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.PLUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        when().payloadFromClasspath("/issue-comment-reaction-strategy-all-error.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554)).comment(any());
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.CONFUSED);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testNone() throws IOException {
        when().payloadFromClasspath("/issue-comment-reaction-strategy-none-success.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554))
                            .comment("hello from @reaction-strategy none");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        when().payloadFromClasspath("/issue-comment-reaction-strategy-none-error.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554)).comment(any());
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testProgress() throws IOException {
        when().payloadFromClasspath("/issue-comment-reaction-strategy-progress-success.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("hello from @reaction-strategy progress");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        when().payloadFromClasspath("/issue-comment-reaction-strategy-progress-error.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554)).comment(any());
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testProgressError() throws IOException {
        when().payloadFromClasspath("/issue-comment-reaction-strategy-progress-error-success.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("hello from @reaction-strategy progress-error");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        when().payloadFromClasspath("/issue-comment-reaction-strategy-progress-error-error.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554)).comment(any());
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.CONFUSED);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
