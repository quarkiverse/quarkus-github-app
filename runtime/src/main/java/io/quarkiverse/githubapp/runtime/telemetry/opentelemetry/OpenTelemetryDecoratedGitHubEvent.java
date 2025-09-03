package io.quarkiverse.githubapp.runtime.telemetry.opentelemetry;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.opentelemetry.api.trace.SpanContext;
import io.quarkiverse.githubapp.GitHubEvent;
import io.vertx.core.json.JsonObject;

class OpenTelemetryDecoratedGitHubEvent implements GitHubEvent {

    private final GitHubEvent originalGitHubEvent;

    private final SpanContext rootSpanContext;

    OpenTelemetryDecoratedGitHubEvent(GitHubEvent originalGitHubEvent, SpanContext rootSpanContext) {
        this.originalGitHubEvent = originalGitHubEvent;
        this.rootSpanContext = rootSpanContext;
    }

    @Override
    public Long getInstallationId() {
        return originalGitHubEvent.getInstallationId();
    }

    @Override
    public Optional<String> getAppName() {
        return originalGitHubEvent.getAppName();
    }

    @Override
    public String getDeliveryId() {
        return originalGitHubEvent.getDeliveryId();
    }

    @Override
    public Optional<String> getRepository() {
        return originalGitHubEvent.getRepository();
    }

    @Override
    @JsonIgnore
    public String getRepositoryOrThrow() {
        return originalGitHubEvent.getRepositoryOrThrow();
    }

    @Override
    public String getEvent() {
        return originalGitHubEvent.getEvent();
    }

    @Override
    public String getAction() {
        return originalGitHubEvent.getAction();
    }

    @Override
    public String getEventAction() {
        return originalGitHubEvent.getEventAction();
    }

    @Override
    public String getPayload() {
        return originalGitHubEvent.getPayload();
    }

    @Override
    @JsonIgnore
    public JsonObject getParsedPayload() {
        return originalGitHubEvent.getParsedPayload();
    }

    @Override
    public boolean isReplayed() {
        return originalGitHubEvent.isReplayed();
    }

    @JsonIgnore
    public SpanContext getRootSpanContext() {
        return rootSpanContext;
    }
}
