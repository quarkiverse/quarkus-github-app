package io.quarkiverse.githubapp.telemetry;

import io.quarkiverse.githubapp.GitHubEvent;

public interface TelemetryMetricsReporter {

    void incrementGitHubEventSuccess(GitHubEvent gitHubEvent);

    void incrementGitHubEventError(GitHubEvent gitHubEvent, Throwable throwable);

    void incrementGitHubEventMethodSuccess(GitHubEvent gitHubEvent, String className, String methodName, String signature);

    void incrementGitHubEventMethodError(GitHubEvent gitHubEvent, String className, String methodName, String signature,
            Throwable throwable);
}
