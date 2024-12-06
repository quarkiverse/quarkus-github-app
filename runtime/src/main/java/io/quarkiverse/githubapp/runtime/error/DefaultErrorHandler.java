package io.quarkiverse.githubapp.runtime.error;

import java.util.ArrayList;
import java.util.List;
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
        List<String> errorMessageParameters = new ArrayList<>();

        errorMessage.append("Error handling delivery {").append(errorMessageParameters.size()).append("}\n");
        errorMessageParameters.add(gitHubEvent.getDeliveryId());
        if (t instanceof ServiceDownException || t instanceof GitHubServiceDownException) {
            errorMessage
                    .append("››› GitHub APIs are not available at the moment. Have a look at https://www.githubstatus.com.\n");
        }
        if (gitHubEvent.getRepository().isPresent()) {
            errorMessage.append("› Repository: {").append(errorMessageParameters.size()).append("}\n");
            errorMessageParameters.add(gitHubEvent.getRepository().get());
        }
        errorMessage.append("› Event:      {").append(errorMessageParameters.size()).append("}\n");
        errorMessageParameters.add(gitHubEvent.getEventAction());

        if (payload != null) {
            Optional<String> context = PayloadHelper.getContext(payload);
            if (context.isPresent()) {
                errorMessage.append("› Context:    {").append(errorMessageParameters.size()).append("}\n");
                errorMessageParameters.add(PayloadHelper.getContext(payload).get());
            }
        }

        if (gitHubEvent.getAppName().isPresent()) {
            errorMessage.append("› Redeliver:  {").append(errorMessageParameters.size()).append("}\n");
            errorMessageParameters.add(String.format(REDELIVERY_URL, gitHubEvent.getAppName().get()));
        }

        if (launchMode.isDevOrTest()) {
            errorMessage.append("› Payload:\n")
                    .append("----\n")
                    .append("{").append(errorMessageParameters.size()).append("}\n")
                    .append("----\n");
            errorMessageParameters.add(gitHubEvent.getParsedPayload().encodePrettily());
        }

        errorMessage.append("Exception");

        LOG.errorv(t, errorMessage.toString(), errorMessageParameters.toArray());
    }

}
