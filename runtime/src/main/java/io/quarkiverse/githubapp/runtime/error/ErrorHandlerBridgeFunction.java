package io.quarkiverse.githubapp.runtime.error;

import java.util.function.BiFunction;

import org.jboss.logging.Logger;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class ErrorHandlerBridgeFunction implements BiFunction<GitHubEvent, Throwable, Void> {

    private static final Logger LOG = Logger.getLogger(ErrorHandlerBridgeFunction.class);

    public static final ErrorHandlerBridgeFunction INSTANCE = new ErrorHandlerBridgeFunction();

    @Override
    public Void apply(GitHubEvent gitHubEvent, Throwable t) {
        try (InstanceHandle<ErrorHandler> errorHandler = Arc.container().instance(ErrorHandler.class)) {
            if (errorHandler.isAvailable()) {
                errorHandler.get().handleError(gitHubEvent, null, t);
            } else {
                LOG.error("An error occurred and no ErrorHandler is available", t);
            }

            throw new GitHubEventDispatchingException(
                    "Error dispatching event: " + gitHubEvent.getDeliveryId() + " of type: " + gitHubEvent.getEventAction(), t);
        }
    }
}
