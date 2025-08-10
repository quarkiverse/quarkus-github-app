package io.quarkiverse.githubapp.runtime.config;

import java.security.PrivateKey;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.Credentials;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig.Debug;
import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig.Telemetry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Startup;

@Singleton
@Startup
public class CheckedConfigProvider {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    private final GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    private final LaunchMode launchMode;

    private final Optional<PrivateKey> privateKey;
    private final Optional<String> webhookSecret;
    private final String webhookUrlPath;

    private final Set<String> missingPropertyKeys = new TreeSet<>();

    @Inject
    CheckedConfigProvider(GitHubAppRuntimeConfig gitHubAppRuntimeConfig, LaunchMode launchMode) {
        this.gitHubAppRuntimeConfig = gitHubAppRuntimeConfig;
        this.launchMode = launchMode;

        Map<String, String> credentials = getCredentials();

        String privateKey = credentials.get(Credentials.PRIVATE_KEY);
        if (privateKey == null || privateKey.isBlank()) {
            privateKey = gitHubAppRuntimeConfig.privateKey().orElse(null);
        }
        this.privateKey = privateKey != null ? Optional.of(new PrivateKeyConverter().convert(privateKey.trim()))
                : Optional.empty();

        String webhookSecretFromCredentials = credentials.get(Credentials.WEBHOOK_SECRET);
        if (webhookSecretFromCredentials != null && !webhookSecretFromCredentials.isBlank()) {
            this.webhookSecret = Optional.of(webhookSecretFromCredentials.trim());
        } else {
            this.webhookSecret = gitHubAppRuntimeConfig.webhookSecret();
        }
        this.webhookUrlPath = gitHubAppRuntimeConfig.webhookUrlPath().startsWith("/") ? gitHubAppRuntimeConfig.webhookUrlPath()
                : "/" + gitHubAppRuntimeConfig.webhookUrlPath();

        if (gitHubAppRuntimeConfig.appId().isEmpty()) {
            missingPropertyKeys.add("quarkus.github-app.app-id (.env: QUARKUS_GITHUB_APP_APP_ID)");
        }
        if (this.privateKey.isEmpty()) {
            missingPropertyKeys.add("quarkus.github-app.private-key (.env: QUARKUS_GITHUB_APP_PRIVATE_KEY)");
        }
        if (launchMode == LaunchMode.NORMAL && this.webhookSecret.isEmpty()) {
            missingPropertyKeys.add("quarkus.github-app.webhook-secret (.env: QUARKUS_GITHUB_APP_WEBHOOK_SECRET)");
        }

        if (launchMode != LaunchMode.TEST) {
            checkConfig();
        }

        if (this.webhookSecret.isPresent() && launchMode.isDevOrTest()) {
            LOG.info("Payload signature checking is disabled in dev and test modes.");
        }
    }

    public String appId() {
        if (launchMode == LaunchMode.TEST) {
            checkConfig();
        }

        // The optional will never be empty; using orElseThrow instead of get to avoid IDE warnings.
        return gitHubAppRuntimeConfig.appId().orElseThrow();
    }

    public Optional<String> appName() {
        return gitHubAppRuntimeConfig.appName();
    }

    public PrivateKey privateKey() {
        if (launchMode == LaunchMode.TEST) {
            checkConfig();
        }

        // The optional will never be empty; using orElseThrow instead of get to avoid IDE warnings.
        return privateKey.orElseThrow();
    }

    public Optional<String> webhookSecret() {
        return webhookSecret;
    }

    public Optional<String> webhookProxyUrl() {
        return gitHubAppRuntimeConfig.webhookProxyUrl();
    }

    public String restApiEndpoint() {
        return gitHubAppRuntimeConfig.restApiEndpoint();
    }

    public String webhookUrlPath() {
        return webhookUrlPath;
    }

    public String graphqlApiEndpoint() {
        return gitHubAppRuntimeConfig.graphqlApiEndpoint();
    }

    public Optional<String> personalAccessToken() {
        return gitHubAppRuntimeConfig.personalAccessToken();
    }

    public boolean checkInstallationTokenValidity() {
        return gitHubAppRuntimeConfig.checkInstallationTokenValidity();
    }

    public Debug debug() {
        return gitHubAppRuntimeConfig.debug();
    }

    public Telemetry telemetry() {
        return gitHubAppRuntimeConfig.telemetry();
    }

    public ConfigFile.Source getEffectiveSource(ConfigFile.Source source) {
        if (source == ConfigFile.Source.DEFAULT) {
            return gitHubAppRuntimeConfig.readConfigFilesFromSourceRepository() ? ConfigFile.Source.SOURCE_REPOSITORY
                    : ConfigFile.Source.CURRENT_REPOSITORY;
        }
        return source;
    }

    public void checkConfig() {
        if (missingPropertyKeys.isEmpty()) {
            return;
        }

        String errorMessage;

        if (launchMode == LaunchMode.TEST) {
            errorMessage = "\n\nMissing values for configuration properties:\n- " + String.join("\n- ", missingPropertyKeys)
                    + "\n\n"
                    + "This configuration is necessary to create the GitHub clients."
                    + "It is optional in tests only if GitHub clients are being mocked using quarkus-github-app-testing.\n\n"
                    + "For more information, see https://docs.quarkiverse.io/quarkus-github-app/dev/testing.html.";
        } else {
            errorMessage = "\n\nMissing values for configuration properties:\n- " + String.join("\n- ", missingPropertyKeys)
                    + "\n\n"
                    + "This configuration is required in " + (launchMode == LaunchMode.NORMAL ? "prod" : "dev") + " mode.\n\n"
                    + "For more information, see:\n- https://docs.quarkiverse.io/quarkus-github-app/dev/register-github-app.html\n- https://docs.quarkiverse.io/quarkus-github-app/dev/create-github-app.html#_initialize_the_configuration";
        }

        throw new GitHubAppConfigurationException(errorMessage);
    }

    private Map<String, String> getCredentials() {
        if (gitHubAppRuntimeConfig.credentialsProvider().isEmpty()) {
            return Map.of();
        }

        String beanName = gitHubAppRuntimeConfig.credentialsProviderName().orElse(null);
        try (InstanceHandle<CredentialsProvider> credentialsProviderInstance = getCredentialsProvider(beanName)) {
            if (credentialsProviderInstance == null) {
                throw new RuntimeException(
                        "Unable to find credentials provider of name " + (beanName == null ? "default" : beanName));
            }

            String keyRingName = gitHubAppRuntimeConfig.credentialsProvider().get();

            return credentialsProviderInstance.get().getCredentials(keyRingName);
        }
    }

    private static InstanceHandle<CredentialsProvider> getCredentialsProvider(String name) {
        ArcContainer container = Arc.container();
        InstanceHandle<CredentialsProvider> credentialsProvider = name != null
                ? container.instance(name)
                : container.instance(CredentialsProvider.class);

        return credentialsProvider;
    }
}
