package io.quarkiverse.githubapp.deployment;

import java.util.Optional;
import java.util.UUID;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.deployment.junit.GitHubMockingQuarkusUnitTest;
import io.quarkiverse.githubapp.event.Label;
import io.quarkiverse.githubapp.runtime.Headers;
import io.restassured.RestAssured;

public class OptionalConfigFileTest {

    private static final String PAYLOAD = "payloads/label-created.json";

    @RegisterExtension
    static final GitHubMockingQuarkusUnitTest config = new GitHubMockingQuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ListeningClass.class)
                    .addAsResource(PAYLOAD))
            .withConfigurationResource("application.properties");

    @Test
    public void testOptionalConfigFile() {
        RestAssured
                .given()
                .header(Headers.X_GITHUB_EVENT, "label")
                .header(Headers.X_GITHUB_DELIVERY, UUID.randomUUID())
                .contentType("application/json")
                .body(Thread.currentThread().getContextClassLoader().getResourceAsStream(PAYLOAD))
                .when().post("/")
                .then()
                .statusCode(200);
    }

    static class ListeningClass {

        void createLabel(@ConfigFile("my-config-file.txt") Optional<String> configuration,
                @Label.Created GHEventPayload.Label labelPayload) {
        }
    }
}
