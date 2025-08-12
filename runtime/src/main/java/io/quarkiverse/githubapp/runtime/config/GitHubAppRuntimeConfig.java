package io.quarkiverse.githubapp.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkiverse.githubapp.Credentials;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.github-app")
public interface GitHubAppRuntimeConfig {

    /**
     * The numeric application id provided by GitHub.
     * <p>
     * Optional for tests, but mandatory in production and dev mode.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> appId();

    /**
     * The GitHub name of the application.
     * <p>
     * Optional, only used for improving the user experience.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> appName();

    /**
     * Read the configuration files from the source repository in case of a fork.
     */
    @WithDefault("false")
    boolean readConfigFilesFromSourceRepository();

    /**
     * The RSA private key.
     * <p>
     * Optional for tests, but mandatory in production and dev mode.
     */
    Optional<String> privateKey();

    /**
     * The webhook URL path on which the GitHub App route is mounted.
     * <p>
     * It defaults to the root {@code /} but it can be configured to another path such as {@code /github-events} to enable
     * deployment alongside other HTTP routes.
     */
    @WithDefault("/")
    @WithConverter(TrimmedStringConverter.class)
    String webhookUrlPath();

    /**
     * The webhook secret if defined in the GitHub UI.
     */
    Optional<String> webhookSecret();

    /**
     * The credentials provider name.
     * <p>
     * This is the name of the "keyring" containing the GitHub App secrets.
     * <p>
     * Key names are defined in {@link Credentials}.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProvider();

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProviderName();

    /**
     * The Smee.io proxy URL used when testing locally.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> webhookProxyUrl();

    /**
     * The GitHub instance endpoint.
     * <p>
     * Defaults to the public github.com instance.
     */
    @WithDefault("https://api.github.com")
    @WithConverter(TrimmedStringConverter.class)
    String instanceEndpoint();

    /**
     * The REST API endpoint.
     * <p>
     * Defaults to the public github.com instance REST API endpoint.
     */
    @WithDefault("${quarkus.github-app.instance-endpoint}")
    @WithConverter(TrimmedStringConverter.class)
    String restApiEndpoint();

    /**
     * The GraphQL API endpoint.
     * <p>
     * Defaults to the public github.com instance GraphQL endpoint.
     */
    @WithDefault("${quarkus.github-app.instance-endpoint}/graphql")
    @WithConverter(TrimmedStringConverter.class)
    String graphqlApiEndpoint();

    /**
     * A personal access token for use with {@code TokenGitHubClients} or when no installation id is provided in the payload.
     * <p>
     * For standard use cases, you will use the installation client which comes with the installation permissions. It can be
     * injected directly in your method.
     * <p>
     * However, if your payload comes from a webhook and doesn't have an installation id, it's handy to be able to use a
     * client authenticated with a personal access token as the application client permissions are very limited.
     * <p>
     * This token will be used to authenticate the clients provided by {@code TokenGitHubClients} and clients
     * authenticated with this personal access token will be automatically provided when injecting {@code GitHub} or
     * {@code DynamicGraphQLClient} in your method, when the payload doesn't provide an installation id.
     */
    Optional<String> personalAccessToken();

    /**
     * Check the validity of the candidate installation token before returning the client for use.
     * <p>
     * By default, we are extra cautious, but we usually cache the token for 50 minutes (they are valid one hour),
     * so it should be safe to disable this check, not accounting for potential GitHub infra issues,
     * where token could be lost/invalidated by mistake on the GitHub side.
     * <p>
     * This saves one API roundtrip when getting the GitHub client, which might help with performances.
     */
    @WithDefault("true")
    boolean checkInstallationTokenValidity();

    /**
     * Telemetry configuration.
     */
    Telemetry telemetry();

    /**
     * Debug configuration.
     */
    Debug debug();

    @ConfigGroup
    interface Debug {

        /**
         * A directory in which the payloads are saved.
         */
        @WithConverter(TrimmedStringConverter.class)
        Optional<Path> payloadDirectory();
    }

    @ConfigGroup
    interface Telemetry {

        /**
         * Whether telemetry span collecting integration is active at runtime or not.
         * <p>
         * Always inactive if the telemetry integration is not present or disabled at build time.
         */
        @WithDefault("true")
        @WithName("traces.active")
        boolean tracesActive();

        /**
         * Whether telemetry metrics integration is active at runtime or not.
         * <p>
         * Always inactive if the telemetry integration is not present or disabled at build time.
         */
        @WithDefault("true")
        @WithName("metrics.active")
        boolean metricsActive();

        /**
         * Whether we record the event JSON payloads in the OpenTelemetry spans.
         * <p>
         * The payload is only recorded in the root {@code github-event} span.
         */
        @WithDefault("false")
        boolean recordEventPayload();
    }
}
