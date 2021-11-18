package io.quarkiverse.githubapp.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.Label;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public class GitHubGraphQLClientInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ListeningClass.class))
            .withConfigurationResource("application.properties");

    @Test
    public void testGitHubGraphQLClientInjection() {
    }

    static class ListeningClass {

        void createLabel(@ConfigFile("my-config-file.txt") String configuration,
                @Label.Created GHEventPayload.Label labelPayload, GitHub gitHub) {
        }

        void editLabel(@ConfigFile("my-config-file.txt") String configuration,
                @Label.Edited GHEventPayload.Label labelPayload, DynamicGraphQLClient gitHubGraphQLClient) {
        }

        void deleteLabel(@ConfigFile("my-config-file.txt") String configuration,
                @Label.Deleted GHEventPayload.Label labelPayload, DynamicGraphQLClient gitHubGraphQLClient, GitHub gitHub) {
        }
    }
}
