package io.quarkiverse.githubapp.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.github-app")
public interface GitHubAppBuildTimeConfig {

    /**
     * Telemetry configuration.
     */
    Telemetry telemetry();

    @ConfigGroup
    interface Telemetry {

        /**
         * Whether telemetry traces integration is active at runtime or not.
         * <p>
         * Defaults to true when the Quarkus OpenTelemetry integration is present and enabled.
         */
        @WithName("traces.enabled")
        Optional<Boolean> tracesEnabled();

        /**
         * Whether telemetry metrics integration is active at runtime or not.
         * <p>
         * Defaults to true when the Quarkus OpenTelemetry integration is present and enabled.
         */
        @WithName("metrics.enabled")
        Optional<Boolean> metricsEnabled();
    }
}
