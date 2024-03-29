package io.quarkiverse.githubapp.runtime.error;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.ServiceDownException;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.runtime.github.GitHubServiceDownException;
import io.quarkiverse.githubapp.runtime.github.PayloadHelper;
import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.LaunchMode;

@ApplicationScoped
@DefaultBean
public class DefaultErrorHandler implements ErrorHandler {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    private static final String REDELIVERY_URL = "https://github.com/settings/apps/%1$s/advanced";

    @Inject
    LaunchMode launchMode;

    @Override
    public void handleError(GitHubEvent gitHubEvent, GHEventPayload payload, Throwable t) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Error handling delivery " + gitHubEvent.getDeliveryId() + "\n");
        if (t instanceof ServiceDownException || t instanceof GitHubServiceDownException) {
            errorMessage
                    .append("››› GitHub APIs are not available at the moment. Have a look at https://www.githubstatus.com.\n");
        }
        if (gitHubEvent.getRepository().isPresent()) {
            errorMessage.append("› Repository: " + gitHubEvent.getRepository().get() + "\n");
        }
        errorMessage.append("› Event:      " + gitHubEvent.getEventAction() + "\n");

        if (payload != null) {
            Optional<String> context = PayloadHelper.getContext(payload);
            if (context.isPresent()) {
                errorMessage.append("› Context:    " + PayloadHelper.getContext(payload).get() + "\n");
            }
        }

        if (gitHubEvent.getAppName().isPresent()) {
            errorMessage.append("› Redeliver:  " + String.format(REDELIVERY_URL, gitHubEvent.getAppName().get()) + "\n");
        }

        if (launchMode.isDevOrTest()) {
            errorMessage.append("› Payload:\n")
                    .append("----\n")
                    .append(gitHubEvent.getParsedPayload().encodePrettily()).append("\n")
                    .append("----\n");
        }

        errorMessage.append("Exception");

        LOG.error(errorMessage.toString(), t);
    }

}
