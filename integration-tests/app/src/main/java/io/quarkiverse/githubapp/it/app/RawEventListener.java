package io.quarkiverse.githubapp.it.app;

import java.io.IOException;

import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.event.RawEvent;

public class RawEventListener {

    void testRawEventListenedTo(@RawEvent(event = "issues", action = "opened") GitHubEvent gitHubEvent, GitHub gitHub)
            throws IOException {
        assert gitHubEvent.getEvent().equals("issues");
        assert gitHubEvent.getAction().equals("opened");

        gitHub.getRepository("test/test").getIssue(1).addLabels("testRawEventListenedTo");
    }

    void testRawEventNotListenedTo(@RawEvent(event = "pull_request", action = "opened") GitHubEvent gitHubEvent, GitHub gitHub)
            throws IOException {
        throw new IllegalStateException("testRawEventNotListened should not have been called");
    }

    void testRawEventNotListenedToCatchAllAction(@RawEvent(event = "pull_request") GitHubEvent gitHubEvent, GitHub gitHub)
            throws IOException {
        throw new IllegalStateException("testRawEventNotListenedCatchAllAction should not have been called");
    }

    void testRawEventCatchAllAction(@RawEvent(event = "issues") GitHubEvent gitHubEvent, GitHub gitHub) throws IOException {
        assert gitHubEvent.getEvent().equals("issues");
        assert gitHubEvent.getAction().equals("opened");

        gitHub.getRepository("test/test").getIssue(1).addLabels("testRawEventCatchAll");
    }

    void testRawEventCatchAllEventAction(@RawEvent GitHubEvent gitHubEvent, GitHub gitHub) throws IOException {
        assert gitHubEvent.getEvent().equals("issues");
        assert gitHubEvent.getAction().equals("opened");

        gitHub.getRepository("test/test").getIssue(1).addLabels("testRawEventCatchAllEventAction");
    }
}
