package io.quarkiverse.githubapp.telemetry;

import io.quarkiverse.githubapp.GitHubEvent;

/**
 * Report spans to OpenTelemetry.
 */
public interface TelemetryTracesReporter {

    void reportEarlyRequestError(String deliveryId, String event, String error);

    TelemetrySpanWrapper createGitHubEventSpan(GitHubEvent gitHubEvent);

    GitHubEvent decorateGitHubEvent(GitHubEvent originalGitHubEvent, TelemetrySpanWrapper spanWrapper);

    TelemetrySpanWrapper createGitHubEventListeningMethodSpan(GitHubEvent gitHubEvent, String className, String methodName,
            String signature);

    void reportSuccess(GitHubEvent gitHubEvent, TelemetrySpanWrapper spanWrapper);

    void reportException(GitHubEvent gitHubEvent, TelemetrySpanWrapper spanWrapper, Throwable e);

    TelemetryScopeWrapper makeCurrent(TelemetrySpanWrapper spanWrapper);

    void endSpan(TelemetrySpanWrapper spanWrapper);

    void reportCommandMethodSuccess(GitHubEvent gitHubEvent, String commandClassName, String commandLine);

    void reportCommandMethodError(GitHubEvent gitHubEvent, String commandClassName,
            String commandLine, CommandErrorType errorType, String errorMessage);

}
