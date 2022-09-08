package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedSearchIterable;
import org.kohsuke.github.ReactionContent;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class TestingFrameworkTest {

    @Inject
    BackgroundProcessor backgroundProcessor;

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

    @Test
    @ExtendWith(MockitoExtension.class) // To get strict stubs, which simplifies verifyNoMoreInteractions() (stubbed calls are verified automatically)
    void clientProvider() {
        List<String> capture = new ArrayList<>();
        // Use case: a background processor goes through all installations of the app,
        // to perform an operation on every single repository.
        BackgroundProcessor.behavior = (clientProvider) -> {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                for (GHRepository repository : installation.listRepositories()) {
                    GitHub installationClient = clientProvider.getInstallationClient(installation.getId());
                    // Get the repository with enhanced permissions thanks to the installation client.
                    repository = installationClient.getRepository(repository.getName());
                    // Simulate doing stuff with the repository.
                    // Normally that stuff would require enhanced permissions,
                    // but here's we're just calling getFullName() to simplify.
                    capture.add(repository.getFullName());
                }
            }
        };

        GHApp app = Mockito.mock(GHApp.class);
        GHAppInstallation installation1 = Mockito.mock(GHAppInstallation.class);
        GHAppInstallation installation2 = Mockito.mock(GHAppInstallation.class);
        GHRepository installation1Repo1 = Mockito.mock(GHRepository.class);
        GHRepository installation2Repo1 = Mockito.mock(GHRepository.class);
        GHRepository installation2Repo2 = Mockito.mock(GHRepository.class);

        assertThatCode(() -> given()
                .github(mocks -> {
                    Mockito.when(mocks.applicationClient().getApp()).thenReturn(app);
                    Mockito.when(installation1.getId()).thenReturn(1L);
                    Mockito.when(installation2.getId()).thenReturn(2L);
                    PagedIterable<GHAppInstallation> appInstallations = MockHelper.mockPagedIterable(installation1,
                            installation2);
                    Mockito.when(app.listInstallations()).thenReturn(appInstallations);

                    Mockito.when(installation1Repo1.getName()).thenReturn("quarkus");
                    PagedSearchIterable<GHRepository> installation1Repos = MockHelper.mockPagedIterable(installation1Repo1);
                    Mockito.when(installation1.listRepositories())
                            .thenReturn(installation1Repos);

                    Mockito.when(installation2Repo1.getName()).thenReturn("quarkus-github-app");
                    Mockito.when(installation2Repo2.getName()).thenReturn("quarkus-github-api");
                    PagedSearchIterable<GHRepository> installation2Repos = MockHelper.mockPagedIterable(installation2Repo1,
                            installation2Repo2);
                    Mockito.when(installation2.listRepositories())
                            .thenReturn(installation2Repos);

                    // Installation clients will return different Repository objects than the application client:
                    // that's expected.
                    Mockito.when(mocks.repository("quarkus").getFullName()).thenReturn("quarkusio/quarkus");
                    Mockito.when(mocks.repository("quarkus-github-app").getFullName())
                            .thenReturn("quarkiverse/quarkus-github-app");
                    Mockito.when(mocks.repository("quarkus-github-api").getFullName())
                            .thenReturn("quarkiverse/quarkus-github-api");
                })
                .when(backgroundProcessor::process)
                .then().github(mocks -> {
                    Mockito.verifyNoMoreInteractions(app, installation1, installation2, installation1Repo1, installation2Repo1,
                            installation2Repo2);
                    Mockito.verifyNoMoreInteractions(mocks.ghObjects());
                }))
                .doesNotThrowAnyException();
        assertThat(capture).containsExactlyInAnyOrder(
                "quarkusio/quarkus",
                "quarkiverse/quarkus-github-app",
                "quarkiverse/quarkus-github-api");
    }

}
