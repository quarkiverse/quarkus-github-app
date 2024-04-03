package io.quarkiverse.githubapp.runtime.config;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Optional;

import io.quarkiverse.githubapp.Credentials;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigRoot(name = "github-app", phase = ConfigPhase.RUN_TIME)
public class GitHubAppRuntimeConfig {

    /**
     * The numeric application id provided by GitHub.
     * <p>
     * Optional for tests, but mandatory in production and dev mode.
     */
    @ConfigItem
    Optional<String> appId;

    /**
     * The GitHub name of the application.
     * <p>
     * Optional, only used for improving the user experience.
     */
    @ConfigItem
    Optional<String> appName;

    /**
     * Read the configuration files from the source repository in case of a fork.
     */
    @ConfigItem(defaultValue = "false")
    boolean readConfigFilesFromSourceRepository;

    /**
     * The RSA private key.
     * <p>
     * Optional for tests, but mandatory in production and dev mode.
     */
    @ConfigItem
    @ConvertWith(PrivateKeyConverter.class)
    Optional<PrivateKey> privateKey;

    /**
     * The webhook secret if defined in the GitHub UI.
     */
    @ConfigItem
    Optional<String> webhookSecret;

    /**
     * The credentials provider name.
     * <p>
     * This is the name of the "keyring" containing the GitHub App secrets.
     * <p>
     * Key names are defined in {@link Credentials}.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    Optional<String> credentialsProvider;

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    Optional<String> credentialsProviderName;

    /**
     * The Smee.io proxy URL used when testing locally.
     */
    @ConfigItem
    Optional<String> webhookProxyUrl;

    /**
     * The GitHub instance endpoint.
     * <p>
     * Defaults to the public github.com instance.
     */
    @ConfigItem(defaultValue = "https://api.github.com")
    String instanceEndpoint;

    /**
     * The REST API endpoint.
     * <p>
     * Defaults to the public github.com instance REST API endpoint.
     */
    @ConfigItem(defaultValue = "${quarkus.github-app.instance-endpoint}")
    String restApiEndpoint;

    /**
     * The GraphQL API endpoint.
     * <p>
     * Defaults to the public github.com instance GraphQL endpoint.
     */
    @ConfigItem(defaultValue = "${quarkus.github-app.instance-endpoint}/graphql")
    String graphqlApiEndpoint;

    /**
     * Debug configuration.
     */
    @ConfigItem
    Debug debug;

    @ConfigGroup
    public static class Debug {

        /**
         * A directory in which the payloads are saved.
         */
        @ConfigItem
        public Optional<Path> payloadDirectory;
    }
}
