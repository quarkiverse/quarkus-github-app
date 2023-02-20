package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppMockito.mockBuilder;
import static io.quarkiverse.githubapp.testing.GitHubAppMockito.mockPagedIterable;
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

import jakarta.inject.Inject;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueCommentQueryBuilder;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedSearchIterable;
import org.kohsuke.github.ReactionContent;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkiverse.githubapp.it.testingframework.config.MyConfigFile;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.arc.Arc;
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
                .event(GHEvent.ISSUES))
                .doesNotThrowAnyException();
        assertThat(capture[0]).isEqualTo("someValue");
    }

    @Test
    void ghObjectVerify() {
        // Do not change this, the documentation includes the exact same code
        ThrowingCallable assertion = () -> when()
                .payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> { // <4>
                    Mockito.verify(mocks.issue(750705278))
                            .comment("Hello from my GitHub App");
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().comment("Hello from my GitHub App");
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().comment("otherValue");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Actual invocations have different arguments:\n" +
                        "GHIssue#750705278.comment(\n    \"otherValue\"\n);");
    }

    @Test
    void ghObjectMockAndVerify() {
        // Do not change this, the documentation includes the exact same code
        ThrowingCallable assertion = () -> given()
                .github(mocks -> {
                    Mockito.doThrow(new RuntimeException("Simulated exception"))
                            .when(mocks.issue(750705278))
                            .comment(Mockito.any());
                })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    Mockito.verify(mocks.issue(750705278))
                            .createReaction(ReactionContent.CONFUSED);
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            try {
                payload.getIssue().comment("Hello from my GitHub App");
            } catch (RuntimeException e) {
                payload.getIssue().createReaction(ReactionContent.CONFUSED);
            }
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().comment("Hello from my GitHub App");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The event handler threw an exception: Simulated exception");
    }

    @Test
    void ghAppMockUtils() {
        // Do not change this, the documentation includes the exact same code
        var queryCommentsBuilder = mockBuilder(GHIssueCommentQueryBuilder.class);
        ThrowingCallable assertion = () -> given()
                .github(mocks -> {
                    Mockito.when(mocks.issue(750705278).queryComments())
                            .thenReturn(queryCommentsBuilder);
                    var previousCommentFromBotMock = mocks.ghObject(GHIssueComment.class, 2);
                    var commentsMock = mockPagedIterable(previousCommentFromBotMock);
                    Mockito.when(queryCommentsBuilder.list())
                            .thenReturn(commentsMock);
                })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    Mockito.verify(mocks.issue(750705278)).queryComments();
                    // The bot already commented, it should not comment again.
                    Mockito.verifyNoMoreInteractions(mocks.issue(750705278));
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            if (!payload.getIssue().queryComments().list().iterator().hasNext()) {
                payload.getIssue().comment("Hello from my GitHub App");
            }
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            boolean ignored = !payload.getIssue().queryComments().list().iterator().hasNext();
            // Comment regardless
            payload.getIssue().comment("Hello from my GitHub App");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("No interactions wanted here");
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
    @ExtendWith(MockitoExtension.class) // To get strict stubs, which simplifies verifyNoMoreInteractions() (stubbed calls are verified automatically)
    void configFileMocking_linting() {
        ThrowingCallable assertion = () -> given().github(mocks -> {
            // The config file from the main branch, passed to the listener
            mocks.configFile("config.yml").fromString("someProperty: \"valueFromConfigFile\"");
            // The config file from the PR, retrieved explicitly through GitHubConfigFileProvider
            mocks.configFile("config.yml")
                    .withRef("09d9cfb430c2192856e62b330145cabfe3610aa1") // HEAD of PR
                    .fromString("someProperty:\n  - \"invalidListValueForStringProperty\"");
        })
                .when().payloadFromClasspath("/pr-opened-dependabot.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(841242879))
                            .getHead();
                    verify(mocks.pullRequest(841242879))
                            .comment(ArgumentMatchers.contains("Linting failed! Error:\n"));
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        // Success (linting detects the invalid content)
        PullRequestEventListener.behavior = (payload, configFile) -> {
            GHPullRequest pr = payload.getPullRequest();
            String sha = pr.getHead().getSha();
            GitHubConfigFileProvider configFileProvider = Arc.container().instance(GitHubConfigFileProvider.class).get();
            try {
                configFileProvider.fetchConfigFile(payload.getRepository(), sha,
                        "config.yml", ConfigFile.Source.CURRENT_REPOSITORY, MyConfigFile.class);
            } catch (IllegalStateException e) {
                pr.comment("Linting failed! Error:\n" + e.getMessage());
            }
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure (no linting)
        PullRequestEventListener.behavior = (payload, configFile) -> {
            // act as the other behavior...
            GHPullRequest pr = payload.getPullRequest();
            String sha = pr.getHead().getSha();
            // ... then forget to lint.
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Wanted but not invoked:\n" +
                        "GHPullRequest#841242879.comment(\n" +
                        "    contains(\"Linting failed! Error:\n\")\n" +
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
        String configFilePath = "foo.yml";
        List<MyConfigFile> capture = new ArrayList<>();
        // Use case: a background processor goes through all installations of the app,
        // to perform an operation on every single repository.
        BackgroundProcessor.behavior = (clientProvider, configFileProvider) -> {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                for (GHRepository repository : installation.listRepositories()) {
                    GitHub installationClient = clientProvider.getInstallationClient(installation.getId());
                    // Get the repository with enhanced permissions thanks to the installation client.
                    repository = installationClient.getRepository(repository.getFullName());
                    // Simulate doing stuff with the repository.
                    // Normally that stuff would require enhanced permissions,
                    // but here's we're just calling getFullName() to simplify.
                    capture.add(configFileProvider.fetchConfigFile(repository, configFilePath, ConfigFile.Source.DEFAULT,
                            MyConfigFile.class).orElse(null));
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
                    PagedIterable<GHAppInstallation> appInstallations = mockPagedIterable(installation1, installation2);
                    Mockito.when(app.listInstallations()).thenReturn(appInstallations);

                    Mockito.when(installation1Repo1.getFullName()).thenReturn("quarkusio/quarkus");
                    PagedSearchIterable<GHRepository> installation1Repos = mockPagedIterable(installation1Repo1);
                    Mockito.when(installation1.listRepositories())
                            .thenReturn(installation1Repos);

                    Mockito.when(installation2Repo1.getFullName()).thenReturn("quarkiverse/quarkus-github-app");
                    Mockito.when(installation2Repo2.getFullName()).thenReturn("quarkiverse/quarkus-github-api");
                    PagedSearchIterable<GHRepository> installation2Repos = mockPagedIterable(installation2Repo1,
                            installation2Repo2);
                    Mockito.when(installation2.listRepositories())
                            .thenReturn(installation2Repos);

                    // Installation clients will return different Repository objects than the application client:
                    // that's expected.
                    mocks.configFile(mocks.repository("quarkusio/quarkus"), configFilePath)
                            .fromString("someProperty: \"quarkus with some text 42\"");
                    mocks.configFile(mocks.repository("quarkiverse/quarkus-github-app"), configFilePath)
                            .fromString("someProperty: \"quarkus-github-app with some text 43\"");
                    mocks.configFile(mocks.repository("quarkiverse/quarkus-github-api"), configFilePath)
                            .fromString("someProperty: \"quarkus-github-api with some text 44\"");
                })
                .when(backgroundProcessor::process)
                .then().github(mocks -> {
                    Mockito.verifyNoMoreInteractions(app, installation1, installation2, installation1Repo1, installation2Repo1,
                            installation2Repo2);
                    Mockito.verifyNoMoreInteractions(mocks.ghObjects());
                }))
                .doesNotThrowAnyException();
        assertThat(capture).containsExactlyInAnyOrder(
                new MyConfigFile("quarkus with some text 42"),
                new MyConfigFile("quarkus-github-app with some text 43"),
                new MyConfigFile("quarkus-github-api with some text 44"));
    }

}
