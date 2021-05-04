package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.testing.GitHubAppTestingResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(GitHubAppTestingResource.class)
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
                    verifyNoMoreInteractions(mocks.ghObjects());
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

}
