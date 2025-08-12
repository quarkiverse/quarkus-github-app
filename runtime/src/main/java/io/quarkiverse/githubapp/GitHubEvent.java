package io.quarkiverse.githubapp;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.json.JsonObject;

public interface GitHubEvent {

    Long getInstallationId();

    Optional<String> getAppName();

    String getDeliveryId();

    Optional<String> getRepository();

    @JsonIgnore
    String getRepositoryOrThrow();

    String getEvent();

    String getAction();

    String getEventAction();

    String getPayload();

    @JsonIgnore
    JsonObject getParsedPayload();

    boolean isReplayed();

    default <T extends GitHubEvent> T as(Class<T> clazz) {
        return (T) this;
    }
}
