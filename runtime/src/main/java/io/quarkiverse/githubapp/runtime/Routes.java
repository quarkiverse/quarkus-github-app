package io.quarkiverse.githubapp.runtime;

import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_DELIVERY;
import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_EVENT;
import static io.quarkiverse.githubapp.runtime.Headers.X_HUB_SIGNATURE;
import static io.quarkiverse.githubapp.runtime.Headers.X_REQUEST_ID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HandlerType;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class Routes {

    private final GitHubService gitHubService;

    private final GitHubEventDispatcher gitHubEventDispatcher;

    @Inject
    Routes(GitHubService gitHubService, GitHubEventDispatcher gitHubEventDispatcher) {
        this.gitHubService = gitHubService;
        this.gitHubEventDispatcher = gitHubEventDispatcher;
    }

    @Route(path = "/", type = HandlerType.BLOCKING, methods = HttpMethod.POST, consumes = "application/json", produces = "application/json")
    public String handleRequest(RoutingContext routingContext,
            @Header(X_REQUEST_ID) String requestId,
            @Header(X_HUB_SIGNATURE) String hubSignature,
            @Header(X_GITHUB_DELIVERY) String gitHubDelivery,
            @Header(X_GITHUB_EVENT) String gitHubEvent) {

        JsonObject body = routingContext.getBodyAsJson();
        Long installationId = extractInstallationId(body);

        gitHubEventDispatcher.dispatch(gitHubService.getInstallationClient(installationId), gitHubEvent,
                body.getString("action"), routingContext.getBodyAsString());

        return "{}";
    }

    private Long extractInstallationId(JsonObject body) {
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
}
