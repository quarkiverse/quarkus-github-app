package ilove.quark.us;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.event.Issue;

class MyGitHubApp {

    void onOpen(@Issue.Opened GHEventPayload.Issue issuePayload) throws IOException {
        issuePayload.getIssue().comment(":wave: Hello from my GitHub App");
    }
}
