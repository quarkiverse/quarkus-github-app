package io.quarkiverse.githubapp.it.app;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;

public class SameNameMethodsEventListener {

    void onEvent(@Issue.Opened GHEventPayload.Issue payload) throws IOException {
        payload.getIssue().addLabels("sameNameIssue");
    }

    void onEvent(@IssueComment.Created GHEventPayload.IssueComment payload) throws IOException {
        payload.getIssue().addLabels("sameNameIssueComment");
    }
}
