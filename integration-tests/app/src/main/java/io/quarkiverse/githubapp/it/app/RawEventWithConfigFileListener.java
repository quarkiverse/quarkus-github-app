package io.quarkiverse.githubapp.it.app;

import java.io.IOException;
import java.util.Optional;

import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.event.RawEvent;

public class RawEventWithConfigFileListener {

    void onRawEventWithConfigFile(@RawEvent(event = "label", action = "created") GitHubEvent gitHubEvent,
            @ConfigFile("config.yml") Optional<String> configFile,
            GitHub gitHub) throws IOException {
        if (configFile.isPresent()) {
            gitHub.getRepository("test/test").getIssue(1).addLabels("testRawEventWithConfigFile");
        }
    }
}
