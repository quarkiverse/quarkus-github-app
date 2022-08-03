package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Using @Inject for composition is deprecated but we have code handling it specifically so let's test it for now.
 */
@QuarkusTest
@GitHubAppTest
public class InjectCompositionCliTest {

    @Test
    void test() throws IOException {
        when().payloadFromClasspath("/issue-comment-inject-composition.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @inject-composition test");
                });

        when().payloadFromClasspath("/issue-comment-inject-composition-verbose.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @inject-composition test - verbose");
                });
    }
}
