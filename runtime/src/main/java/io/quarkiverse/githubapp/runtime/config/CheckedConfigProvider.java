package io.quarkiverse.githubapp.runtime.config;

import java.security.PrivateKey;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig.Debug;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Startup;

@Singleton
@Startup
public class CheckedConfigProvider {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    private final GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    private final LaunchMode launchMode;

    private final Set<String> missingPropertyKeys = new TreeSet<>();

    @Inject
    CheckedConfigProvider(GitHubAppRuntimeConfig gitHubAppRuntimeConfig, LaunchMode launchMode) {
        this.gitHubAppRuntimeConfig = gitHubAppRuntimeConfig;
        this.launchMode = launchMode;

        if (gitHubAppRuntimeConfig.appId.isEmpty()) {
            missingPropertyKeys.add("quarkus.github-app.app-id (.env: QUARKUS_GITHUB_APP_APP_ID)");
        }
        if (gitHubAppRuntimeConfig.privateKey.isEmpty()) {
            missingPropertyKeys.add("quarkus.github-app.private-key (.env: QUARKUS_GITHUB_APP_PRIVATE_KEY)");
        }
        if (launchMode == LaunchMode.NORMAL && gitHubAppRuntimeConfig.webhookSecret.isEmpty()) {
            missingPropertyKeys.add("quarkus.github-app.webhook-secret (.env: QUARKUS_GITHUB_APP_WEBHOOK_SECRET)");
        }

        if (launchMode != LaunchMode.TEST) {
            checkConfig();
        }

        if (gitHubAppRuntimeConfig.webhookSecret.isPresent() && launchMode.isDevOrTest()) {
            LOG.info("Payload signature checking is disabled in dev and test modes.");
        }
    }

    public String appId() {
        if (launchMode == LaunchMode.TEST) {
            checkConfig();
        }

        // The optional will never be empty; using orElseThrow instead of get to avoid IDE warnings.
        return gitHubAppRuntimeConfig.appId.orElseThrow();
    }

    public Optional<String> appName() {
        return gitHubAppRuntimeConfig.appName;
    }

    public PrivateKey privateKey() {
        if (launchMode == LaunchMode.TEST) {
            checkConfig();
        }

        // The optional will never be empty; using orElseThrow instead of get to avoid IDE warnings.
        return gitHubAppRuntimeConfig.privateKey.orElseThrow();
    }

    public Optional<String> webhookSecret() {
        return gitHubAppRuntimeConfig.webhookSecret;
    }

    public Optional<String> webhookProxyUrl() {
        return gitHubAppRuntimeConfig.webhookProxyUrl;
    }

    public String restApiEndpoint() {
        return gitHubAppRuntimeConfig.restApiEndpoint;
    }

    public String graphqlApiEndpoint() {
        return gitHubAppRuntimeConfig.graphqlApiEndpoint;
    }

    public Debug debug() {
        return gitHubAppRuntimeConfig.debug;
    }

    public ConfigFile.Source getEffectiveSource(ConfigFile.Source source) {
        if (source == ConfigFile.Source.DEFAULT) {
            return gitHubAppRuntimeConfig.readConfigFilesFromSourceRepository ? ConfigFile.Source.SOURCE_REPOSITORY
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
}
