package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class ServiceInjectionCliTest {

    @Test
    void testServiceInjection() throws IOException {
        when().payloadFromClasspath("/issue-comment-service-injection-test.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @service-injection test");
                });
    }
}
