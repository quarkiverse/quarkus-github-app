package io.quarkiverse.githubapp.runtime;

import java.util.List;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class GitHubAppRecorder {

    private final RuntimeValue<GitHubAppRuntimeConfig> config;

    public GitHubAppRecorder(RuntimeValue<GitHubAppRuntimeConfig> config) {
        this.config = config;
    }

    public Handler<RoutingContext> replayUiHandler(String replayUiFinalDestination, String replayUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            ShutdownContext shutdownContext) {
        WebJarStaticHandler handler = new WebJarStaticHandler(replayUiFinalDestination, replayUiPath,
                webRootConfigurations);
        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));

        return handler;
    }

    public void configureRetry() {
        config.getValue().rateLimit().retry().maxAttempts().ifPresent(maxAttempts -> System
                .setProperty("org.kohsuke.github.GitHubClient.retryCount", String.valueOf(maxAttempts)));
    }
}
