package io.quarkiverse.githubapp.runtime;

public final class Headers {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_HUB_SIGNATURE_256 = "X-Hub-Signature-256";
    public static final String X_GITHUB_EVENT = "X-GitHub-Event";
    public static final String X_GITHUB_DELIVERY = "X-GitHub-Delivery";
    public static final String X_QUARKIVERSE_GITHUB_APP_REPLAYED = "X-Quarkiverse-GitHub-App-Replayed";
    public static final String X_QUARKIVERSE_GITHUB_APP_ORIGINAL_DELIVERY = "X-Quarkiverse-GitHub-App-Original-Delivery";

    public static final String[] FORWARDED_HEADERS = {
            CONTENT_TYPE,
            X_REQUEST_ID,
            X_HUB_SIGNATURE_256,
            X_GITHUB_EVENT,
            X_GITHUB_DELIVERY
    };

    private Headers() {
    }
}
