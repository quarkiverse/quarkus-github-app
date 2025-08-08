package io.quarkiverse.githubapp.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.github-app")
public interface GitHubAppBuildTimeConfig {

    /**
     * Eventing model used to determine how to handle the events received from GitHub.
     * <ul>
     * <li>When using {@code async}, events are handled asynchronously and a 200 is returned right away so you can't see the
     * errors on the GitHub side.
     * The advantage of using this approach is that you don't keep the HTTP connection to GitHub open while your payload is
     * handled.</li>
     * <li>When using {@code sync}, events are handled synchronously and errors correctly reported to GitHub if an error
     * occurs.</li>
     * </ul>
     */
    @WithDefault("async")
    EventingModel eventingModel();

    enum EventingModel {
        SYNC,
        ASYNC;
    }
}
