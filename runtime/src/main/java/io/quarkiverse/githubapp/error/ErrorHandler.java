package io.quarkiverse.githubapp.error;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.GitHubEvent;

public interface ErrorHandler {

    /**
     * Note that the payload might be null if the error happened before the events have been dispatched with a payload.
     */
    void handleError(GitHubEvent gitHubEvent, GHEventPayload payload, Throwable t);
}
