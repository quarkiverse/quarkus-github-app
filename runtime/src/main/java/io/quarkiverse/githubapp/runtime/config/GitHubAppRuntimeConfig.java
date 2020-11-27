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
     */
    @ConfigItem
    public String appId;

    /**
     * The GitHub name of the application.
     * <p>
     * Optional, only used for improving the user experience.
     */
    @ConfigItem
    public Optional<String> appName;

    /**
     * The RSA private key.
     */
    @ConfigItem
    @ConvertWith(PrivateKeyConverter.class)
    public PrivateKey privateKey;

    /**
     * The webhook secret if defined in the GitHub UI.
     */
    @ConfigItem
    public Optional<String> webhookSecret;

    /**
     * The Smee.io proxy URL used when testing locally.
     */
    @ConfigItem
    public Optional<String> webhookProxyUrl;

    /**
     * Debug configuration.
     */
    @ConfigItem
    public Debug debug;

    @ConfigGroup
    public static class Debug {

        /**
         * A directory in which the payloads are saved.
         */
        @ConfigItem
        public Optional<Path> payloadDirectory;
    }
}
