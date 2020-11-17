package io.quarkiverse.githubapp.runtime;

import org.kohsuke.github.GitHub;

public interface GitHubEventDispatcher {

    public void dispatch(GitHub gitHub, String gitHubEvent, String action, String payload);
}
