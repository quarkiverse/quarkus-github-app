package io.quarkiverse.githubapp.deployment;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.TokenGitHubClients;
import io.quarkiverse.githubapp.event.Label;
import io.quarkus.test.QuarkusExtensionTest;

public class TokenGitHubClientsInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ListeningClass.class).addClass(ListeningClass2.class))
            .withConfigurationResource("application-token.properties");

    @Test
    public void testGitHubGraphQLClientInjection() {
    }

    static class ListeningClass {

        @Inject
        TokenGitHubClients tokenGitHubClients;

        void createLabel(@Label.Created GHEventPayload.Label labelPayload) {
        }
    }

    static class ListeningClass2 {

        void createLabel(@Label.Created GHEventPayload.Label labelPayload, TokenGitHubClients tokenGitHubClients) {
        }
    }
}
