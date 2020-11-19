package io.quarkiverse.githubapp.runtime;

public class GitHubEvent {

    private final Long installationId;

    private final String deliveryId;

    private final String repository;

    private final String event;

    private final String action;

    private final String payload;

    public GitHubEvent(Long installationId, String deliveryId, String repository, String event, String action, String payload) {
        this.installationId = installationId;
        this.deliveryId = deliveryId;
        this.repository = repository;
        this.event = event;
        this.action = action;
        this.payload = payload;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getRepository() {
        return repository;
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

    @Override
    public String toString() {
        return "GitHubEvent [installationId=" + installationId + ", deliveryId=" + deliveryId + ", repository=" + repository
                + ", event=" + event + ", action=" + action + ", payload=" + payload + "]";
    }
}
