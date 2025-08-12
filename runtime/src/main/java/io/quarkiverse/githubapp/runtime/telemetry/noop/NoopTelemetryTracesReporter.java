package io.quarkiverse.githubapp.runtime.telemetry.noop;

import jakarta.inject.Singleton;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.telemetry.TelemetryScopeWrapper;
import io.quarkiverse.githubapp.telemetry.TelemetrySpanWrapper;
import io.quarkiverse.githubapp.telemetry.TelemetryTracesReporter;
import io.quarkus.arc.DefaultBean;

@Singleton
@DefaultBean
public class NoopTelemetryTracesReporter implements TelemetryTracesReporter {

    @Override
    public void reportEarlyRequestError(String deliveryId, String event, String error) {
        // noop
    }

    @Override
    public TelemetrySpanWrapper createGitHubEventSpan(GitHubEvent gitHubEvent) {
        return NoopTelemetrySpanWrapper.INSTANCE;
    }

    @Override
    public GitHubEvent decorateGitHubEvent(GitHubEvent originalGitHubEvent, TelemetrySpanWrapper spanWrapper) {
        return originalGitHubEvent;
    }

    @Override
    public TelemetrySpanWrapper createGitHubEventListeningMethodSpan(GitHubEvent gitHubEvent, String className,
            String methodName, String signature) {
        return NoopTelemetrySpanWrapper.INSTANCE;
    }

    @Override
    public void reportSuccess(GitHubEvent gitHubEvent, TelemetrySpanWrapper spanWrapper) {
        // noop
    }

    @Override
    public void reportException(GitHubEvent gitHubEvent, TelemetrySpanWrapper spanWrapper, Throwable e) {
        // noop
    }

    @Override
    public TelemetryScopeWrapper makeCurrent(TelemetrySpanWrapper spanWrapper) {
        return NoopTelemetryScopeWrapper.INSTANCE;
    }

    @Override
    public void endSpan(TelemetrySpanWrapper spanWrapper) {
        // noop
    }

    private static class NoopTelemetrySpanWrapper implements TelemetrySpanWrapper {

        private static final NoopTelemetrySpanWrapper INSTANCE = new NoopTelemetrySpanWrapper();
    }

    private static class NoopTelemetryScopeWrapper implements TelemetryScopeWrapper {

        private static final NoopTelemetryScopeWrapper INSTANCE = new NoopTelemetryScopeWrapper();

        @Override
        public void close() throws Exception {
            // noop
        }
    }
}
