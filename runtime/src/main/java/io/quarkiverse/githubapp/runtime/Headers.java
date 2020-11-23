package io.quarkiverse.githubapp.runtime;

public final class Headers {

    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_HUB_SIGNATURE_256 = "X-Hub-Signature-256";
    public static final String X_GITHUB_EVENT = "X-GitHub-Event";
    public static final String X_GITHUB_DELIVERY = "X-GitHub-Delivery";

    public static final String[] FORWARDED_HEADERS = {
            X_REQUEST_ID,
            X_HUB_SIGNATURE_256,
            X_GITHUB_EVENT,
            X_GITHUB_DELIVERY
    };

    private Headers() {
    }
}
