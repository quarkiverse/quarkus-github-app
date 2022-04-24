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
public class DefaultCliTest {

    @Test
    void testBasic() throws IOException {
        when().payloadFromClasspath("/issue-comment-default-basic.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @default basic");
                });
    }

    @Test
    void testBasicOtherAlias() throws IOException {
        when().payloadFromClasspath("/issue-comment-default-basic-other-alias.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @default basic");
                });
    }

    @Test
    void testCommandWithArguments() throws IOException {
        when().payloadFromClasspath("/issue-comment-default-command-with-arguments.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @default command-with-arguments @gsmet");
                });
    }

    @Test
    void testGroup1Command1() throws IOException {
        when().payloadFromClasspath("/issue-comment-default-group1-command1.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @default group1 command1");
                });
    }

    @Test
    void testGroup1Command2() throws IOException {
        when().payloadFromClasspath("/issue-comment-default-group1-command2.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "hello from @default group1 command2");
                });
    }
}
