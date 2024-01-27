package io.quarkiverse.githubapp.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.event.RawEvent;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidRawEventTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(InvalidRawEventListeningClass.class))
            .assertException(e -> {
                assertThat(e.getMessage()).contains(
                        "Parameter subscribing to a GitHub raw event must be of type 'io.quarkiverse.githubapp.GitHubEvent'");
            })
            .withConfigurationResource("application.properties");

    @Test
    public void testInvalidRawEvent() {
    }

    static class InvalidRawEventListeningClass {

        void rawEvent(@RawEvent(event = "pull_request", action = "opened") GHEventPayload.PullRequest pullRequestPayload) {
        }
    }
}
