package io.quarkiverse.githubapp;

import jakarta.inject.Singleton;

import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

@Singleton
public class TokenGitHubClients {

    private final GitHubService gitHubService;

    TokenGitHubClients(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    public GitHub getRestClient() {
        return gitHubService.getTokenRestClient();
    }

    public DynamicGraphQLClient getGraphQLClient() {
        return gitHubService.getTokenGraphQLClient();
    }
}
