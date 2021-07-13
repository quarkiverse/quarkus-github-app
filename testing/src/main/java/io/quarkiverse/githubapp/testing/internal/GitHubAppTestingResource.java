package io.quarkiverse.githubapp.testing.internal;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class GitHubAppTestingResource implements QuarkusTestResourceLifecycleManager {

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
