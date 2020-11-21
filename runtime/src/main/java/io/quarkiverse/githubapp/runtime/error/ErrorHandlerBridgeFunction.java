package io.quarkiverse.githubapp.runtime.error;

import java.util.function.Function;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkus.arc.Arc;

public class ErrorHandlerBridgeFunction implements Function<Throwable, Void> {

    private final GitHubEvent gitHubEvent;

    private final GHEventPayload payload;

    public ErrorHandlerBridgeFunction(GitHubEvent gitHubEvent) {
        this.gitHubEvent = gitHubEvent;
        this.payload = null;
    }

    public ErrorHandlerBridgeFunction(GitHubEvent gitHubEvent, GHEventPayload payload) {
        this.gitHubEvent = gitHubEvent;
        this.payload = payload;
    }

    @Override
    public Void apply(Throwable t) {
        Arc.container().instance(ErrorHandler.class).get().handleError(gitHubEvent, payload, t);
        return null;
    }

}
