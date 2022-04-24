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
public class HelpCliTest {

    @Test
    void testHelp() throws IOException {
        when().payloadFromClasspath("/issue-comment-help.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyCommandExecution(mocks, "usage: @help <command> [ <args> ]\n"
                            + "\n"
                            + "Commands are:\n"
                            + "    command1   Command 1\n"
                            + "    help       Push a message with help\n"
                            + "\n"
                            + "See '@help help <command>' for more information on a specific command.\n");
                });
    }
}
