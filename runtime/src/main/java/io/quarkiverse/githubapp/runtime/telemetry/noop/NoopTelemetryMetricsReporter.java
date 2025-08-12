package io.quarkiverse.githubapp.runtime.telemetry.noop;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.telemetry.TelemetryMetricsReporter;

public class NoopTelemetryMetricsReporter implements TelemetryMetricsReporter {

    @Override
    public void incrementGitHubEventSuccess(GitHubEvent gitHubEvent) {
        // noop
    }

    @Override
    public void incrementGitHubEventError(GitHubEvent gitHubEvent, Throwable throwable) {
        // noop
    }

    @Override
    public void incrementGitHubEventMethodSuccess(GitHubEvent gitHubEvent, String className, String methodName,
            String signature) {
        // noop
    }

    @Override
    public void incrementGitHubEventMethodError(GitHubEvent gitHubEvent, String className, String methodName, String signature,
            Throwable throwable) {
        // noop
    }
}
