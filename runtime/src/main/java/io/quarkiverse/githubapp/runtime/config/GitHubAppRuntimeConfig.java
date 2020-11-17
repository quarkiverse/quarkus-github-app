package io.quarkiverse.githubapp.runtime.config;

import java.security.PrivateKey;
import java.util.Optional;

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
}
