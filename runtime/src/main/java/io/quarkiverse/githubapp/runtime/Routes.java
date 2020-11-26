package io.quarkiverse.githubapp.runtime;

import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_DELIVERY;
import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_EVENT;
import static io.quarkiverse.githubapp.runtime.Headers.X_HUB_SIGNATURE_256;
import static io.quarkiverse.githubapp.runtime.Headers.X_REQUEST_ID;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HandlerType;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class Routes {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    @Inject
    Event<GitHubEvent> gitHubEventEmitter;

    @Inject
    GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    @Inject
    PayloadSignatureChecker payloadSignatureChecker;

    @Inject
    LaunchMode launchMode;

    public void init(@Observes StartupEvent startupEvent) {
        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && launchMode.isDevOrTest()) {
            LOG.warn("Payload signature checking is disabled in dev and test modes.");
        }
    }

    @Route(path = "/", type = HandlerType.BLOCKING, methods = HttpMethod.POST, consumes = "application/json", produces = "application/json")
    public void handleRequest(RoutingContext routingContext,
            RoutingExchange routingExchange,
            @Header(X_REQUEST_ID) String requestId,
            @Header(X_HUB_SIGNATURE_256) String hubSignature,
            @Header(X_GITHUB_DELIVERY) String deliveryId,
            @Header(X_GITHUB_EVENT) String event) {

        if (isBlank(deliveryId) || isBlank(hubSignature)) {
            routingExchange.response().setStatusCode(400).end();
            return;
        }

        JsonObject body = routingContext.getBodyAsJson();

        if (body == null) {
            routingExchange.ok().end();
            return;
        }

        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && !launchMode.isDevOrTest()) {
            if (!payloadSignatureChecker.matches(routingContext.getBody().getBytes(), hubSignature)) {
                StringBuilder signatureError = new StringBuilder("Invalid signature for delivery: ").append(deliveryId)
                        .append("\n");
                signatureError.append("› Signature: ").append(hubSignature).append("\n");
                signatureError.append("› Payload:\n")
                        .append("----\n")
                        .append(routingContext.getBodyAsString()).append("\n")
                        .append("----");
                LOG.error(signatureError.toString());

                routingExchange.response().setStatusCode(400).end("Invalid signature.");
                return;
            }
        }

        Long installationId = extractInstallationId(body);
        String repository = extractRepository(body);
        GitHubEvent gitHubEvent = new GitHubEvent(installationId, gitHubAppRuntimeConfig.appName.orElse(null), deliveryId,
                repository, event, body.getString("action"), routingContext.getBodyAsString(), body);

        gitHubEventEmitter.fire(gitHubEvent);

        routingExchange.ok().end();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Long extractInstallationId(JsonObject body) {
        Long installationId;

        JsonObject installation = body.getJsonObject("installation");
        if (installation != null) {
            installationId = installation.getLong("id");
            if (installationId != null) {
                return installationId;
            }
        }

        throw new IllegalStateException("Unable to extract installation id from payload");
    }

    private static String extractRepository(JsonObject body) {
        JsonObject repository = body.getJsonObject("repository");
        if (repository == null) {
            return null;
        }

        return repository.getString("full_name");
    }
}
