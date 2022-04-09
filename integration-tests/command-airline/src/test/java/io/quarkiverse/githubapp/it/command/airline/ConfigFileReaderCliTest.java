package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
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
public class ConfigFileReaderCliTest {

    public static final String HELLO = "hello from config file reader";

    @Test
    void testConfigFileReader() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                "config-file-reader.yml",
                "hello: " + HELLO))
                .when().payloadFromClasspath("/issue-comment-config-file-reader-test.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.ROCKET);
                    verify(mocks.issue(1168785554))
                            .comment(HELLO);
                    verify(mocks.issueComment(1093016219))
                            .createReaction(ReactionContent.PLUS_ONE);
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }
}
