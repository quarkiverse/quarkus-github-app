package io.quarkiverse.githubapp.it.testingframework;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;

@ApplicationScoped
public class BackgroundProcessor {

    public static Behavior behavior;

    @Inject
    GitHubClientProvider clientProvider;

    @Inject
    GitHubConfigFileProvider configFileProvider;

    public void process() throws IOException {
        behavior.execute(clientProvider, configFileProvider);
    }

    public interface Behavior {
        void execute(GitHubClientProvider clientProvider, GitHubConfigFileProvider configFileProvider) throws IOException;
    }
}
