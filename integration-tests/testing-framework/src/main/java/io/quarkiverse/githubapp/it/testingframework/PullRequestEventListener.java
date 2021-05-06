package io.quarkiverse.githubapp.it.testingframework;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkiverse.githubapp.it.testingframework.config.MyConfigFile;

class PullRequestEventListener {

    public static Behavior behavior;

    void onEvent(@PullRequest.Opened GHEventPayload.PullRequest payload,
            @ConfigFile("config.yml") MyConfigFile configFile) throws IOException {
        behavior.execute(payload, configFile);
    }

    public interface Behavior {
        void execute(GHEventPayload.PullRequest payload, MyConfigFile configFile) throws IOException;
    }
}
