package io.quarkiverse.githubapp.it.testingframework;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.it.testingframework.config.MyConfigFile;

class IssueEventListener {

    public static Behavior behavior;

    void onEvent(@Issue.Opened GHEventPayload.Issue payload,
            @ConfigFile("config.yml") MyConfigFile configFile) throws IOException {
        behavior.execute(payload, configFile);
    }

    public interface Behavior {
        void execute(GHEventPayload.Issue payload, MyConfigFile configFile) throws IOException;
    }
}
