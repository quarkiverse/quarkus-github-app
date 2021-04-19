package io.quarkiverse.githubapp.runtime;

import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_DELIVERY;
import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_EVENT;
import static io.quarkiverse.githubapp.runtime.Headers.X_HUB_SIGNATURE_256;
import static io.quarkiverse.githubapp.runtime.Headers.X_QUARKIVERSE_GITHUB_APP_REPLAYED;
import static io.quarkiverse.githubapp.runtime.Headers.X_REQUEST_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkiverse.githubapp.runtime.replay.ReplayEventsRoute;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HandlerType;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class Routes {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Inject
    Event<GitHubEvent> gitHubEventEmitter;

    @Inject
    GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    @Inject
    PayloadSignatureChecker payloadSignatureChecker;

    @Inject
    LaunchMode launchMode;

    @Inject
    Instance<ReplayEventsRoute> replayRouteInstance;

    @Inject
    HttpConfiguration httpConfig;

    Path tmpDirectory;

    public void init(@Observes StartupEvent startupEvent) throws IOException {
        Json.mapper.registerModule(new Jdk8Module());
        Json.prettyMapper.registerModule(new Jdk8Module());

        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && launchMode.isDevOrTest()) {
            LOG.info("Payload signature checking is disabled in dev and test modes.");
        }

        if (gitHubAppRuntimeConfig.debug.payloadDirectory.isPresent()) {
            Files.createDirectories(gitHubAppRuntimeConfig.debug.payloadDirectory.get());
            LOG.warn("Payloads saved to: "
                    + gitHubAppRuntimeConfig.debug.payloadDirectory.get().toAbsolutePath().toString());
        }
    }

    @Route(path = "/", type = HandlerType.BLOCKING, methods = HttpMethod.POST, consumes = "application/json", produces = "application/json")
    public void handleRequest(RoutingContext routingContext,
            RoutingExchange routingExchange,
            @Header(X_REQUEST_ID) String requestId,
            @Header(X_HUB_SIGNATURE_256) String hubSignature,
            @Header(X_GITHUB_DELIVERY) String deliveryId,
            @Header(X_GITHUB_EVENT) String event,
            @Header(X_QUARKIVERSE_GITHUB_APP_REPLAYED) String replayed) throws IOException {

        if (!launchMode.isDevOrTest() && (isBlank(deliveryId) || isBlank(hubSignature))) {
            routingExchange.response().setStatusCode(400).end();
            return;
        }

        JsonObject body = routingContext.getBodyAsJson();

        if (body == null) {
            routingExchange.ok().end();
            return;
        }

        byte[] bodyBytes = routingContext.getBody().getBytes();
        String action = body.getString("action");

        if (!isBlank(deliveryId) && gitHubAppRuntimeConfig.debug.payloadDirectory.isPresent()) {
            String fileName = DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + event + "-"
                    + (!isBlank(action) ? action + "-" : "") + deliveryId + ".json";
            Files.write(gitHubAppRuntimeConfig.debug.payloadDirectory.get().resolve(fileName), bodyBytes);
        }

        Long installationId = extractInstallationId(body);
        String repository = extractRepository(body);
        GitHubEvent gitHubEvent = new GitHubEvent(installationId, gitHubAppRuntimeConfig.appName.orElse(null), deliveryId,
                repository, event, action, routingContext.getBodyAsString(), body, "true".equals(replayed) ? true : false);

        if (launchMode == LaunchMode.DEVELOPMENT && replayRouteInstance.isResolvable()) {
            replayRouteInstance.get().pushEvent(gitHubEvent);
        }

        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && !launchMode.isDevOrTest()) {
            if (!payloadSignatureChecker.matches(bodyBytes, hubSignature)) {
                StringBuilder signatureError = new StringBuilder("Invalid signature for delivery: ").append(deliveryId)
                        .append("\n");
                signatureError.append("› Signature: ").append(hubSignature);
                LOG.error(signatureError.toString());

                routingExchange.response().setStatusCode(400).end("Invalid signature.");
                return;
            }
        }

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
