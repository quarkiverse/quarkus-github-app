package io.quarkiverse.githubapp.runtime.replay;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class ReplayUiStaticHandler implements Handler<RoutingContext> {

    private String replayUiFinalDestination;
    private String replayUiPath;

    public ReplayUiStaticHandler() {
    }

    public ReplayUiStaticHandler(String replayUiFinalDestination, String replayUiPath) {
        this.replayUiFinalDestination = replayUiFinalDestination;
        this.replayUiPath = replayUiPath;
    }

    public String getreplayUiFinalDestination() {
        return replayUiFinalDestination;
    }

    public void setreplayUiFinalDestination(String replayUiFinalDestination) {
        this.replayUiFinalDestination = replayUiFinalDestination;
    }

    public String getreplayUiPath() {
        return replayUiPath;
    }

    public void setreplayUiPath(String replayUiPath) {
        this.replayUiPath = replayUiPath;
    }

    @Override
    public void handle(RoutingContext event) {
        StaticHandler staticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                .setWebRoot(replayUiFinalDestination)
                .setDefaultContentEncoding("UTF-8");

        if (event.normalizedPath().length() == replayUiPath.length()) {
            event.response().setStatusCode(302);
            event.response().headers().set(HttpHeaders.LOCATION, replayUiPath + "/");
            event.response().end();
            return;
        } else if (event.normalizedPath().length() == replayUiPath.length() + 1) {
            event.reroute(replayUiPath + "/index.html");
            return;
        }

        staticHandler.handle(event);
    }

}
