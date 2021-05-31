package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Answers;

import io.quarkiverse.githubapp.testing.GithubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GithubAppTest(defaultAnswers = Answers.RETURNS_DEEP_STUBS)
public class TestingFrameworkDeepStubTest {

    @Test
    void deepStubMock() {
        IssueEventListener.behavior = (payload, configFile) -> {
            GHRepository repo = payload.getIssue().getRepository();
            repo
                    .createContent()
                    .content("dummy")
                    .commit();
        };
        assertThatCode(() -> when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                })).doesNotThrowAnyException();
    }
}
