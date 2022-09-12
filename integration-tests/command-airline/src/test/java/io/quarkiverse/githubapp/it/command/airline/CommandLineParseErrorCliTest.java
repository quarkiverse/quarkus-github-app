package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class CommandLineParseErrorCliTest {

    @Test
    void testParseError() throws IOException {
        when().payloadFromClasspath("/issue-comment-command-line-parse-error-invalid.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issue(1168785554))
                            .comment(
                                    ":warning: We were not able to parse command: @command-line-parse-error invalid-command \"test\" \"foo\n"
                                            + "\n"
                                            + "Errors:\n"
                                            + "\n"
                                            + "- unbalanced quotes in @command-line-parse-error invalid-command \"test\" \"foo\n"
                                            + "\n"
                                            + "Help:\n"
                                            + "\n"
                                            + "```\n"
                                            + "usage: @command-line-parse-error <command> [ <args> ]\n"
                                            + "\n"
                                            + "Commands are:\n"
                                            + "    command1   Command 1\n"
                                            + "```");
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.CONFUSED);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
