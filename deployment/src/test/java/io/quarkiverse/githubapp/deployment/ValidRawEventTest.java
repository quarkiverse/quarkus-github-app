package io.quarkiverse.githubapp.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkiverse.githubapp.event.RawEvent;
import io.quarkus.test.QuarkusUnitTest;

public class ValidRawEventTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ValidRawEventListeningClass.class))
            .withConfigurationResource("application.properties");

    @Test
    public void testInvalidRawEvent() {
    }

    static class ValidRawEventListeningClass {

        void rawEvent(@RawEvent(event = "pull_request", action = "opened") GitHubEvent gitHubEvent) {
        }

        void rawEventCatchAll(@RawEvent GitHubEvent gitHubEvent) {
        }

        void eventRaw(@PullRequest.Opened GitHubEvent gitHubEvent) {
        }
    }
}
