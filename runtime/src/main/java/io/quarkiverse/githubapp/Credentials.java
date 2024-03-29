package io.quarkiverse.githubapp;

import io.quarkus.credentials.CredentialsProvider;

/**
 * The name of the credentials to use in your {@link CredentialsProvider}.
 */
public final class Credentials {

    public static final String PRIVATE_KEY = "githubAppPrivateKey";
    public static final String WEBHOOK_SECRET = "githubAppWebhookSecret";

}
