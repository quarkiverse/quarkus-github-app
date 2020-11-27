package io.quarkiverse.githubapp.runtime;

import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_DELIVERY;
import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_EVENT;
import static io.quarkiverse.githubapp.runtime.Headers.X_HUB_SIGNATURE_256;
import static io.quarkiverse.githubapp.runtime.Headers.X_REQUEST_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Inject
    Event<GitHubEvent> gitHubEventEmitter;

    @Inject
    GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    @Inject
    PayloadSignatureChecker payloadSignatureChecker;

    @Inject
    LaunchMode launchMode;

    Path tmpDirectory;

    public void init(@Observes StartupEvent startupEvent) throws IOException {
        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && launchMode.isDevOrTest()) {
            LOG.warn("Payload signature checking is disabled in dev and test modes.");
        }

        if (gitHubAppRuntimeConfig.debug.payloadDirectory.isPresent()) {
            Files.createDirectories(gitHubAppRuntimeConfig.debug.payloadDirectory.get());
            LOG.warn("Payloads saved in directory: "
                    + gitHubAppRuntimeConfig.debug.payloadDirectory.get().toAbsolutePath().toString());
        }

        tmpDirectory = Files.createTempDirectory("github-app-");
    }

    @Route(path = "/", type = HandlerType.BLOCKING, methods = HttpMethod.POST, consumes = "application/json", produces = "application/json")
    public void handleRequest(RoutingContext routingContext,
            RoutingExchange routingExchange,
            @Header(X_REQUEST_ID) String requestId,
            @Header(X_HUB_SIGNATURE_256) String hubSignature,
            @Header(X_GITHUB_DELIVERY) String deliveryId,
            @Header(X_GITHUB_EVENT) String event) throws IOException {

        if (!launchMode.isDevOrTest() && (isBlank(deliveryId) || isBlank(hubSignature))) {
            routingExchange.response().setStatusCode(400).end();
            return;
        }

        byte[] bodyBytes = routingContext.getBody().getBytes();

        if (!isBlank(deliveryId) && gitHubAppRuntimeConfig.debug.payloadDirectory.isPresent()) {
            String fileName = DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + event + "-" + deliveryId + ".json";
            Files.write(gitHubAppRuntimeConfig.debug.payloadDirectory.get().resolve(fileName), bodyBytes);
        }

        JsonObject body = routingContext.getBodyAsJson();

        if (body == null) {
            routingExchange.ok().end();
            return;
        }

        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && !launchMode.isDevOrTest()) {
            if (!payloadSignatureChecker.matches(bodyBytes, hubSignature)) {
                StringBuilder signatureError = new StringBuilder("Invalid signature for delivery: ").append(deliveryId)
                        .append("\n");
                signatureError.append("› Signature: ").append(hubSignature);
                LOG.error(signatureError.toString());

                // temporary to debug issues
                String fileName = DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + event + "-" + body.getString("action") + "-"
                        + deliveryId + ".json";
                LOG.warn("› Saving payload to: " + fileName);
                Files.write(tmpDirectory.resolve(fileName), bodyBytes);

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

    @Route(path = "/payload/:file", type = HandlerType.BLOCKING, methods = HttpMethod.GET, produces = "application/json")
    public void handleRequest(RoutingContext routingContext, RoutingExchange routingExchange) throws IOException {
        String fileName = routingContext.pathParam("file");
        if (!isBlank(fileName)) {
            Path filePath = tmpDirectory.resolve(fileName);
            if (Files.isReadable(filePath)) {
                routingExchange.ok().sendFile(filePath.toAbsolutePath().toFile().getPath());
                return;
            }
        }
        routingExchange.serverError().end();
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
