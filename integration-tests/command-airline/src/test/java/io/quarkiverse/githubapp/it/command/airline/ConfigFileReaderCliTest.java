package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

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
                    verifyCommandExecution(mocks, HELLO);
                });
    }
}
