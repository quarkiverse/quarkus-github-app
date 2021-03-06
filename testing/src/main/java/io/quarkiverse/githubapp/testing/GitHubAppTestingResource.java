package io.quarkiverse.githubapp.testing;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingCallback;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * @deprecated Annotate your test with {@link GitHubAppTest @GitHubAppTest} instead.
 */
@Deprecated
public final class GitHubAppTestingResource implements QuarkusTestResourceLifecycleManager {

    private io.quarkiverse.githubapp.testing.internal.GitHubAppTestingResource delegate = new io.quarkiverse.githubapp.testing.internal.GitHubAppTestingResource();

    @Override
    public Map<String, String> start() {
        Map<String, String> configProperties = new HashMap<>();
        GitHubAppTestingCallback.enable(configProperties);
        return configProperties;
    }

    @Override
    public void stop() {
        // Nothing to do
    }

}
