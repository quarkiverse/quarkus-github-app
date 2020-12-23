package io.quarkiverse.githubapp;

import java.util.Optional;

import io.vertx.core.json.JsonObject;

public class GitHubEvent {

    private final Long installationId;

    private final Optional<String> appName;

    private final String deliveryId;

    private final Optional<String> repository;

    private final String event;

    private final String action;

    private final String payload;

    private final JsonObject parsedPayload;

    private final boolean replayed;

    public GitHubEvent(Long installationId, String appName, String deliveryId, String repository, String event, String action,
            String payload, JsonObject parsedPayload, boolean replayed) {
        this.installationId = installationId;
        this.appName = Optional.ofNullable(appName);
        this.deliveryId = deliveryId;
        this.repository = Optional.ofNullable(repository);
        this.event = event;
        this.action = action;
        this.payload = payload;
        this.parsedPayload = parsedPayload;
        this.replayed = replayed;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public Optional<String> getAppName() {
        return appName;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Optional<String> getRepository() {
        return repository;
    }

    public String getEvent() {
        return event;
    }

    public String getAction() {
        return action;
    }

    public String getEventAction() {
        StringBuilder sb = new StringBuilder();
        if (event != null && !event.isBlank()) {
            sb.append(event);
        }
        if (action != null && !action.isBlank()) {
            sb.append(".").append(action);
        }
        return sb.toString();
    }

    public String getPayload() {
        return payload;
    }

    public JsonObject getParsedPayload() {
        return parsedPayload;
    }

    public boolean isReplayed() {
        return replayed;
    }

    @Override
    public String toString() {
        return "GitHubEvent [installationId=" + installationId + ", deliveryId=" + deliveryId + ", repository=" + repository
                + ", event=" + event + ", action=" + action + ", payload=" + payload + ", replayed=" + replayed + "]";
    }
}
