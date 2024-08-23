package io.quarkiverse.githubapp.it.app;

import java.io.IOException;

import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.event.RawEvent;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public class RawEventListenerWithoutInstallation {

    public static final String EVENT_TYPE = "sponsor";

    void testRawEventListenerWithoutInstallation(@RawEvent(event = EVENT_TYPE, action = "sponsored") GitHubEvent gitHubEvent,
            GitHub gitHub,
            DynamicGraphQLClient graphQLClient) throws IOException {
        assert gitHubEvent.getEvent().equals(EVENT_TYPE);
        assert gitHubEvent.getAction().equals("sponsored");

        assert graphQLClient != null;
        assert gitHub != null;

        gitHub.getRepository("test/test").getIssue(1).addLabels("testRawEventListenerWithoutInstallation");
    }
}
