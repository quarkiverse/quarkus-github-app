package io.quarkiverse.githubapp;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This object will be serialized to JSON when the replay is enabled.
 * <p>
 * Thus you need to be extra careful adding the proper {@link JsonIgnore} annotations.
 */
public class GitHubEvent {

    private final Long installationId;

    private final Optional<String> appName;

    private final String deliveryId;

    private final Optional<String> repository;

    private final String event;

    private final String action;

    private final String payload;

    private final boolean replayed;

    public GitHubEvent(Long installationId, String appName, String deliveryId, String repository, String event, String action,
            String payload, boolean replayed) {
        this.installationId = installationId;
        this.appName = Optional.ofNullable(appName);
        this.deliveryId = deliveryId;
        this.repository = Optional.ofNullable(repository);
        this.event = event;
        this.action = action;
        this.payload = payload;
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

    @JsonIgnore
    public String getRepositoryOrThrow() {
        return repository
                .orElseThrow(() -> new IllegalStateException("The payload did not provide any repository information"));
    }

    public String getEvent() {
        return event;
    }

    public String getAction() {
        return action;
    }

    @JsonIgnore
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

    public boolean isReplayed() {
        return replayed;
    }

    @Override
    public String toString() {
        return "GitHubEvent [installationId=" + installationId + ", deliveryId=" + deliveryId + ", repository=" + repository
                + ", event=" + event + ", action=" + action + ", payload=" + payload + ", replayed=" + replayed + "]";
    }
}
