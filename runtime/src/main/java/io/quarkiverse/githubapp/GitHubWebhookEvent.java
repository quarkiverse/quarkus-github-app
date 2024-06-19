package io.quarkiverse.githubapp;

import io.vertx.core.json.JsonObject;

public class GitHubWebhookEvent {

    private final String deliveryId;

    private final String event;

    private final String action;

    private final String payload;

    private final JsonObject parsedPayload;

    private final boolean replayed;

    public GitHubWebhookEvent(String deliveryId, String event, String action,
            String payload, JsonObject parsedPayload, boolean replayed) {
        this.deliveryId = deliveryId;
        this.event = event;
        this.action = action;
        this.payload = payload;
        this.parsedPayload = parsedPayload;
        this.replayed = replayed;
    }

    public String getEvent() {
        return event;
    }

    public String getAction() {
        return action;
    }

    public String getPayload() {
        return payload;
    }

    public JsonObject getParsedPayload() {
        if (parsedPayload == null) {
            throw new IllegalStateException("getParsedPayload() may not be called on GitHubEvents that have been serialized");
        }

        return parsedPayload;
    }

    public boolean isReplayed() {
        return replayed;
    }

    @Override
    public String toString() {
        return "GitHubWebhookEvent [deliveryId=" + deliveryId + ", event=" + event
                + ", action=" + action + ", payload=" + payload + ", replayed=" + replayed + "]";
    }
}
