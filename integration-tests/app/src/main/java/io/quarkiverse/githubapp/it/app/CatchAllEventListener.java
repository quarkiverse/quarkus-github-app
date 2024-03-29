package io.quarkiverse.githubapp.it.app;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.event.Issue;

public class CatchAllEventListener {

    void testCatchAll(@Issue GHEventPayload.Issue payload, GitHubEvent gitHubEvent) throws IOException {
        payload.getIssue().addLabels("testCatchAll");

        assert gitHubEvent.getEvent().equals("issues");
        assert gitHubEvent.getAction().equals("opened");
    }
}
