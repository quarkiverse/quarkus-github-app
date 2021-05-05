package io.quarkiverse.githubapp.testing.internal;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.error.DefaultErrorHandler;

// For some reason we need to extend DefaultErrorHandler to mock the ErrorHandler... don't ask me why.
public class CapturingErrorHandler extends DefaultErrorHandler {
    public Throwable captured;

    @Override
    public void handleError(GitHubEvent gitHubEvent, GHEventPayload payload, Throwable t) {
        captured = t;
    }
}
