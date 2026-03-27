package io.quarkiverse.githubapp.it.app;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class SameNameMethodsTest {

    @Test
    void testIssueOpenedMethodIsCalled() throws IOException {
        given().github(mocks -> {
            when(mocks.repository("test/test").getIssue(1))
                    .thenReturn(mocks.issue(1L));
        })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("sameNameIssue");
                });
    }

    @Test
    void testIssueCommentCreatedMethodIsCalled() throws IOException {
        given().github(mocks -> {
            when(mocks.repository("test/test").getIssue(1))
                    .thenReturn(mocks.issue(1L));
        })
                .when().payloadFromClasspath("/issue-comment-created.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(850000001))
                            .addLabels("sameNameIssueComment");
                });
    }
}
