package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.it.command.airline.CdiInjectionCli.Service1;
import io.quarkiverse.githubapp.it.command.airline.CdiInjectionCli.Service2;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class CdiInjectionCliTest {

    @Test
    void testCdiInjection() throws IOException {
        when().payloadFromClasspath("/issue-comment-cdi-injection-test.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, Service1.HELLO + " - " + Service2.HELLO);
                });
    }
}
