package io.quarkiverse.githubapp.it.app;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class OrganizationFilterTest {

    @Test
    void testEventWithoutOrganizationIsRejected() throws IOException {
        assertThat(given()
                .when().payloadFromClasspath("/issue-opened-no-org.json")
                .event(GHEvent.ISSUES)
                .statusCode()).isEqualTo(403);
    }

    @Test
    void testEventWithWrongOrganizationIsRejected() throws IOException {
        assertThat(given()
                .when().payloadFromClasspath("/issue-opened-wrong-org.json")
                .event(GHEvent.ISSUES)
                .statusCode()).isEqualTo(403);
    }
}
