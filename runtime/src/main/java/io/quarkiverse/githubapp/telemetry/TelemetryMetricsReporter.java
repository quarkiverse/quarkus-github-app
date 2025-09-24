package io.quarkiverse.githubapp.telemetry;

import io.quarkiverse.githubapp.GitHubEvent;

/**
 * Report metrics to OpenTelemetry.
 */
public interface TelemetryMetricsReporter {

    void incrementGitHubEventSuccess(GitHubEvent gitHubEvent);

    void incrementGitHubEventError(GitHubEvent gitHubEvent, Throwable throwable);

    void incrementGitHubEventMethodSuccess(GitHubEvent gitHubEvent, String className, String methodName, String signature);

    void incrementGitHubEventMethodError(GitHubEvent gitHubEvent, String className, String methodName, String signature,
            Throwable throwable);

    void incrementCommandMethodSuccess(GitHubEvent gitHubEvent, String commandClassName);

    void incrementCommandMethodError(GitHubEvent gitHubEvent, String commandClassName, CommandErrorType errorType,
            String errorMessage);
}
