package io.quarkiverse.githubapp.error;

import io.quarkiverse.githubapp.GitHubEvent;

public class GitHubEventDeliveryException extends RuntimeException {

    public GitHubEventDeliveryException(GitHubEvent gitHubEvent, String context, boolean serviceDown, Throwable cause) {
        super(generateMessage(gitHubEvent, context, serviceDown, cause), cause);
    }

    private static String generateMessage(GitHubEvent gitHubEvent, String context, boolean serviceDown, Throwable cause) {
        return """
                Error handling a GitHub event:

                > Repository:   %s
                > Event:        %s
                > Delivery ID:  %s
                > Context:      %s
                > Error:        %s
                """.formatted(gitHubEvent.getRepository().orElse(""), gitHubEvent.getEventAction(), gitHubEvent.getDeliveryId(),
                context,
                serviceDown ? "GitHub APIs are unavailable: " + cause.getMessage() : cause.getMessage());
    }
}
