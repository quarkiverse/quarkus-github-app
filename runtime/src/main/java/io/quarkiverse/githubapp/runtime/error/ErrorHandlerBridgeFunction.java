package io.quarkiverse.githubapp.runtime.error;

import java.util.function.Function;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class ErrorHandlerBridgeFunction implements Function<Throwable, Void> {

    private static final Logger LOG = Logger.getLogger(ErrorHandlerBridgeFunction.class);

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
        InstanceHandle<ErrorHandler> errorHandler = Arc.container().instance(ErrorHandler.class);

        if (errorHandler.isAvailable()) {
            errorHandler.get().handleError(gitHubEvent, payload, t);
        } else {
            LOG.error("An error occurred and no ErrorHandler is available", t);
        }

        return null;
    }

}
