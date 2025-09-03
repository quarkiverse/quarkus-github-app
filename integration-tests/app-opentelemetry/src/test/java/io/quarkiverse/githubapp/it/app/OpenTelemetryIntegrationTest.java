package io.quarkiverse.githubapp.it.app;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@GitHubAppTest
public class OpenTelemetryIntegrationTest {

    @Test
    void testTelemetryIntegration() throws IOException {
        GitHubAppTesting.given().github(mocks -> {
            when(mocks.repository("test/test").getIssue(1))
                    .thenReturn(mocks.issue(1L));
        })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.repository("test/test"), times(1))
                            .getIssue(1);
                    verify(mocks.issue(1))
                            .addLabels("testGitHubEvent");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
        GitHubAppTesting.given().github(mocks -> {
            when(mocks.repository("test/test").getIssue(1))
                    .thenReturn(mocks.issue(1L));
        })
                .when().payloadFromClasspath("/issue-closed.json")
                .event(GHEvent.ISSUES, true)
                .then().github(mocks -> {
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        RestAssured.given()
                .when()
                .get("/opentelemetry/exporter/spans")
                .prettyPeek()
                .then()
                .statusCode(200);

        RestAssured.given()
                .when()
                .get("/opentelemetry/exporter/metrics")
                .prettyPeek()
                .then()
                .statusCode(200);
    }
}
