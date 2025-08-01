package io.quarkiverse.githubapp.runtime;

import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_DELIVERY;
import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_EVENT;
import static io.quarkiverse.githubapp.runtime.Headers.X_HUB_SIGNATURE_256;
import static io.quarkiverse.githubapp.runtime.Headers.X_QUARKIVERSE_GITHUB_APP_REPLAYED;
import static io.quarkiverse.githubapp.runtime.Headers.X_REQUEST_ID;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.runtime.replay.ReplayEventsRoute;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.RoutingExchange;
import io.quarkus.vertx.web.runtime.RoutingExchangeImpl;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Singleton
public class Routes {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Inject
    Event<GitHubEvent> gitHubEventEmitter;

    @Inject
    CheckedConfigProvider checkedConfigProvider;

    @Inject
    PayloadSignatureChecker payloadSignatureChecker;

    @Inject
    LaunchMode launchMode;

    @Inject
    Instance<ReplayEventsRoute> replayRouteInstance;

    public void init(@Observes StartupEvent startupEvent) throws IOException {
        if (checkedConfigProvider.debug().payloadDirectory().isPresent()) {
            Files.createDirectories(checkedConfigProvider.debug().payloadDirectory().get());
            LOG.warn("Payloads saved to: "
                    + checkedConfigProvider.debug().payloadDirectory().get().toAbsolutePath().toString());
        }
    }

    public void init(@Observes Router router) {
        router.post(checkedConfigProvider.webhookUrlPath())
                .handler(BodyHandler.create()) // this is required so that the body to be read by subsequent handlers
                .blockingHandler(routingContext -> {
                    handleRequest(
                            routingContext,
                            new RoutingExchangeImpl(routingContext),
                            routingContext.request().getHeader(X_REQUEST_ID),
                            routingContext.request().getHeader(X_HUB_SIGNATURE_256),
                            routingContext.request().getHeader(X_GITHUB_DELIVERY),
                            routingContext.request().getHeader(X_GITHUB_EVENT),
                            routingContext.request().getHeader(X_QUARKIVERSE_GITHUB_APP_REPLAYED));
                });
    }

    private void handleRequest(RoutingContext routingContext,
            RoutingExchange routingExchange,
            String requestId,
            String hubSignature,
            String deliveryId,
            String event,
            String replayed) {

        if (!launchMode.isDevOrTest() && (isBlank(deliveryId) || isBlank(hubSignature))) {
            routingExchange.response().setStatusCode(400).end();
            return;
        }

        if (routingContext.body().buffer() == null) {
            routingExchange.ok().end();
            return;
        }

        byte[] bodyBytes = routingContext.body().buffer().getBytes();

        if (checkedConfigProvider.webhookSecret().isPresent() && !launchMode.isDevOrTest()) {
            if (!payloadSignatureChecker.matches(bodyBytes, hubSignature)) {
                StringBuilder signatureError = new StringBuilder("Invalid signature for delivery: ").append(deliveryId)
                        .append("\n");
                signatureError.append("› Signature: ").append(hubSignature);
                LOG.error(signatureError.toString());

                routingExchange.response().setStatusCode(400).end("Invalid signature.");
                return;
            }
        }

        if (bodyBytes.length == 0) {
            routingExchange.ok().end();
            return;
        }

        String payload = new String(bodyBytes, StandardCharsets.UTF_8);
        JsonObject payloadObject = (JsonObject) Json.decodeValue(payload);

        String action = payloadObject.getString("action");

        if (!isBlank(deliveryId) && checkedConfigProvider.debug().payloadDirectory().isPresent()) {
            String fileName = DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + event + "-"
                    + (!isBlank(action) ? action + "-" : "") + deliveryId + ".json";
            Path path = checkedConfigProvider.debug().payloadDirectory().get().resolve(fileName);
            try {
                Files.write(path, bodyBytes);
            } catch (Exception e) {
                LOG.warnf(e, "Unable to write debug payload: %s", path);
            }
        }

        Long installationId = extractInstallationId(payloadObject);
        String repository = extractRepository(payloadObject);
        GitHubEvent gitHubEvent = new GitHubEvent(installationId, checkedConfigProvider.appName().orElse(null), deliveryId,
                repository, event, action, payload, payloadObject, "true".equals(replayed));

        if (launchMode == LaunchMode.DEVELOPMENT && replayRouteInstance.isResolvable()) {
            replayRouteInstance.get().pushEvent(gitHubEvent);
        }

        gitHubEventEmitter.fire(gitHubEvent);

        routingExchange.ok().end();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public Long extractInstallationId(JsonObject body) {
        Long installationId;

        JsonObject installation = body.getJsonObject("installation");
        if (installation != null) {
            installationId = installation.getLong("id");
            if (installationId != null) {
                return installationId;
            }
        }

        return null;
    }

    private static String extractRepository(JsonObject body) {
        JsonObject repository = body.getJsonObject("repository");
        if (repository == null) {
            return null;
        }

        return repository.getString("full_name");
    }
}
