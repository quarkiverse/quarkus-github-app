package io.quarkiverse.githubapp.it.app;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class IssueOpenedTest {

    @Test
    void testListenersMethodsAreCalled() throws IOException {
        given().github(mocks -> {
            when(mocks.repository("test/test").getIssue(1))
                    .thenReturn(mocks.issue(1L));
        })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("testCatchAll");
                    verify(mocks.repository("test/test"), times(4))
                            .getIssue(1);
                    verify(mocks.issue(1))
                            .addLabels("testGitHubEvent");
                    verify(mocks.issue(1))
                            .addLabels("testRawEventListenedTo");
                    verify(mocks.issue(1))
                            .addLabels("testRawEventCatchAll");
                    verify(mocks.issue(1))
                            .addLabels("testRawEventCatchAllEventAction");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
