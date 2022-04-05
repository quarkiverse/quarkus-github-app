package io.quarkiverse.githubapp.it.app;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.event.Issue;

class CatchAllEventListener {

    void onEvent(@Issue GHEventPayload.Issue payload) throws IOException {
        payload.getIssue().addLabels("someValue");
    }
}
