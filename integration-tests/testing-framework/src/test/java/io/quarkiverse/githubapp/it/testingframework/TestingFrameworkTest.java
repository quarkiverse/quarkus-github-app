package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.ReactionContent;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class TestingFrameworkTest {

    @Test
    void ghObjectMocking() {
        String[] capture = new String[1];
        IssueEventListener.behavior = (payload, configFile) -> {
            capture[0] = payload.getIssue().getBody();
        };
        assertThatCode(() -> given()
                .github(mocks -> {
                    Mockito.when(mocks.issue(750705278).getBody()).thenReturn("someValue");
                })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                }))
                        .doesNotThrowAnyException();
        assertThat(capture[0]).isEqualTo("someValue");
    }

    @Test
    void ghObjectVerify() {
        ThrowingCallable assertion = () -> when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("someValue");
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("someValue");
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("otherValue");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Actual invocations have different arguments:\n" +
                        "GHIssue#750705278.addLabels(\"otherValue\");");
    }

    @Test
    void ghObjectVerifyNoMoreInteractions() {
        ThrowingCallable assertion = () -> when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("someValue");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("someValue");
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("someValue");
            payload.getIssue().addLabels("otherValue");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll("No interactions wanted here:",
                        "-> at io.quarkiverse.githubapp.it.testingframework.TestingFrameworkTest.lambda$ghObjectVerifyNoMoreInteractions$",
                        "But found this interaction on mock 'GHIssue#750705278':",
                        "-> at io.quarkiverse.githubapp.it.testingframework.TestingFrameworkTest.lambda$ghObjectVerifyNoMoreInteractions$");
    }

    @Test
    void configFileMocking() {
        ThrowingCallable assertion = () -> given().github(mocks -> mocks.configFileFromString(
                "config.yml",
                "someProperty: valueFromConfigFile"))
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("valueFromConfigFile");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels(configFile.someProperty);
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("notValueFromConfigFile");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Argument(s) are different! Wanted:\n" +
                        "GHIssue#750705278.addLabels(\n" +
                        "    \"valueFromConfigFile\"\n" +
                        ");");
    }

    @Test
    void missingMock() {
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().getComments().get(0).createReaction(ReactionContent.EYES);
        };
        assertThatThrownBy(() -> when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("someValue");
                    verifyNoMoreInteractions(mocks.ghObjects());
                }))
                        .hasMessageContaining("The event handler threw an exception:")
                        .hasMessageEndingWith("null")
                        .hasStackTraceContaining("at org.kohsuke.github.GHIssue.getComments");
    }

    @Test
    void noDeepStubMock() throws IOException {
        IssueEventListener.behavior = (payload, configFile) -> {
            GHRepository repo = payload.getIssue().getRepository();
            repo.createContent().content("dummy").commit();
        };
        assertThatCode(() -> when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                })).hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void getUserLogin() {
        String[] capture = new String[1];
        PullRequestEventListener.behavior = (payload, configFile) -> {
            if (payload.getPullRequest().getUser() != null) {
                capture[0] = payload.getPullRequest().getUser().getLogin();
            }
        };

        assertThatCode(() -> given()
                .when().payloadFromClasspath("/pr-opened-dependabot.json")
                .event(GHEvent.PULL_REQUEST))
                        .doesNotThrowAnyException();
        assertThat(capture[0]).isEqualTo("dependabot[bot]");
    }

}
