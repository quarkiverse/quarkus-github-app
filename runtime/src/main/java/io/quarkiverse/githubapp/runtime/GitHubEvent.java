package io.quarkiverse.githubapp.runtime;

public class GitHubEvent {

    private Long installationId;

    private String event;

    private String action;

    private String payload;

    public GitHubEvent(Long installationId, String event, String action, String payload) {
        this.installationId = installationId;
        this.event = event;
        this.action = action;
        this.payload = payload;
    }

    public Long getInstallationId() {
        return installationId;
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
        return "GitHubEvent [installationId=" + installationId + ", event=" + event + ", action=" + action + ", payload="
                + payload + "]";
    }
}
