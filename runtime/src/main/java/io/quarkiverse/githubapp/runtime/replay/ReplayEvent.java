package io.quarkiverse.githubapp.runtime.replay;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkus.vertx.web.ReactiveRoutes;

public class ReplayEvent implements ReactiveRoutes.ServerSentEvent<GitHubEvent> {

    private final String event;
    private final GitHubEvent gitHubEvent;

    public ReplayEvent() {
        this.event = "ping";
        this.gitHubEvent = null;
    }

    public ReplayEvent(GitHubEvent gitHubEvent) {
        this.event = "github-event";
        this.gitHubEvent = gitHubEvent;
    }

    @Override
    public GitHubEvent data() {
        return gitHubEvent;
    }

    @Override
    public String event() {
        return event;
    }
}
