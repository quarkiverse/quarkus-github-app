package io.quarkiverse.githubapp.runtime;

import java.util.List;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class GitHubAppRecorder {

    public Handler<RoutingContext> replayUiHandler(String replayUiFinalDestination, String replayUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            ShutdownContext shutdownContext) {
        WebJarStaticHandler handler = new WebJarStaticHandler(replayUiFinalDestination, replayUiPath,
                webRootConfigurations);
        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));

        return handler;
    }
}
