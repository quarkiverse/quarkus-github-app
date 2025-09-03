package io.quarkiverse.githubapp.it.app.opentelemetry;

import java.io.IOException;

import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.event.Issue;

public class GitHubEventListener {

    void issueOpened(@Issue.Opened GitHubEvent gitHubEvent, GitHub gitHub) throws IOException {
        assert gitHubEvent.getEvent().equals("issues");
        assert gitHubEvent.getAction().equals("opened");

        gitHub.getRepository("test/test").getIssue(1).addLabels("testGitHubEvent");
    }

    void issueClosed(@Issue.Closed GitHubEvent gitHubEvent, GitHub gitHub) throws IOException {
        throw new RuntimeException("An expected error to test error path");
    }
}
