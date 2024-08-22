package io.quarkiverse.githubapp.it.app;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class RawEventWithoutInstallationTest {

    @Test
    void testRawEventWithoutInstallation() throws IOException {
        given().github(mocks -> {
            when(mocks.repository("test/test").getIssue(1))
                    .thenReturn(mocks.issue(1L));
        })
                .when().payloadFromClasspath("/event-without-installation.json")
                .rawEvent(RawEventListenerWithoutInstallation.EVENT_TYPE)
                .then().github(mocks -> {
                    verify(mocks.issue(1))
                            .addLabels("testRawEventListenerWithoutInstallation");
                    verify(mocks.issue(1))
                            .addLabels("testRawEventCatchAllEventAction");
                });
    }
}
