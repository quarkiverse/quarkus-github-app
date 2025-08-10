package io.quarkiverse.githubapp.runtime;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkiverse.githubapp.GitHubEvent;
import io.vertx.core.json.JsonObject;

/**
 * This object will be serialized to JSON when the replay is enabled.
 * <p>
 * Thus you need to be extra careful adding the proper {@link JsonIgnore} annotations.
 * <p>
 * This object is also used in the Replay UI's Javascript code so be careful when updating it.
 */
public class SimpleGitHubEvent implements GitHubEvent {

    private final Long installationId;

    private final Optional<String> appName;

    private final String deliveryId;

    private final Optional<String> repository;

    private final String event;

    private final String action;

    private final String eventAction;

    private final String payload;

    private final JsonObject parsedPayload;

    private final boolean replayed;

    public SimpleGitHubEvent(Long installationId, String appName, String deliveryId, String repository, String event,
            String action,
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

        StringBuilder eventActionSb = new StringBuilder();
        if (event != null && !event.isBlank()) {
            eventActionSb.append(event);
        }
        if (action != null && !action.isBlank()) {
            eventActionSb.append(".").append(action);
        }
        this.eventAction = eventActionSb.toString();
    }

    @Override
    public Long getInstallationId() {
        return installationId;
    }

    @Override
    public Optional<String> getAppName() {
        return appName;
    }

    @Override
    public String getDeliveryId() {
        return deliveryId;
    }

    @Override
    public Optional<String> getRepository() {
        return repository;
    }

    @JsonIgnore
    @Override
    public String getRepositoryOrThrow() {
        return repository
                .orElseThrow(() -> new IllegalStateException("The payload did not provide any repository information"));
    }

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public String getEventAction() {
        return eventAction;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @JsonIgnore
    @Override
    public JsonObject getParsedPayload() {
        if (parsedPayload == null) {
            throw new IllegalStateException("getParsedPayload() may not be called on GitHubEvents that have been serialized");
        }

        return parsedPayload;
    }

    @Override
    public boolean isReplayed() {
        return replayed;
    }

    @Override
    public String toString() {
        return "GitHubEvent [installationId=" + installationId + ", deliveryId=" + deliveryId + ", repository=" + repository
                + ", event=" + event + ", action=" + action + ", payload=" + payload + ", replayed=" + replayed + "]";
    }
}
