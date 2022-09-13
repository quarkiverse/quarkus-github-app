package io.quarkiverse.githubapp.runtime.config;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;

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
