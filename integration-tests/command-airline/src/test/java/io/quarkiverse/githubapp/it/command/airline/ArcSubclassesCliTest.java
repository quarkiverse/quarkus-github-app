package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
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
public class ArcSubclassesCliTest {

    @Test
    void testApplicationScopedScopeIssues() throws IOException {
        when().payloadFromClasspath("/issue-comment-arc-application-scoped-command1-issue.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("hello from @arc application-scoped-command1");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.PLUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
        when().payloadFromClasspath("/issue-comment-arc-application-scoped-command1-pr.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testApplicationScopedScopeIssuesAndPullRequests() throws IOException {
        when().payloadFromClasspath("/issue-comment-arc-application-scoped-command2-issue.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @arc application-scoped-command2");
                });
        when().payloadFromClasspath("/issue-comment-arc-application-scoped-command2-pr.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @arc application-scoped-command2");
                });
    }

    @Test
    void testSubclassScopeIssues() throws IOException {
        when().payloadFromClasspath("/issue-comment-arc-subclass-command1-issue.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment("hello from @arc subclass-command1");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.PLUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
        when().payloadFromClasspath("/issue-comment-arc-subclass-command1-pr.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testSubclassScopeIssuesAndPullRequests() throws IOException {
        when().payloadFromClasspath("/issue-comment-arc-subclass-command2-issue.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @arc subclass-command2");
                });
        when().payloadFromClasspath("/issue-comment-arc-subclass-command2-pr.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @arc subclass-command2");
                });
    }
}
