package io.quarkiverse.githubapp.runtime;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public class MultiplexedEvent {

    private final GHEventPayload payload;

    private final GitHub gitHub;

    private final DynamicGraphQLClient gitHubGraphQLClient;

    public MultiplexedEvent(GHEventPayload payload, GitHub gitHub, DynamicGraphQLClient gitHubGraphQLClient) {
        this.payload = payload;
        this.gitHub = gitHub;
        this.gitHubGraphQLClient = gitHubGraphQLClient;
    }

    public GHEventPayload getPayload() {
        return payload;
    }

    public GitHub getGitHub() {
        return gitHub;
    }

    public DynamicGraphQLClient getGitHubGraphQLClient() {
        if (gitHubGraphQLClient == null) {
            throw new IllegalStateException("The GraphQL client has not been initialized and should not be accessed.");
        }

        return gitHubGraphQLClient;
    }
}
