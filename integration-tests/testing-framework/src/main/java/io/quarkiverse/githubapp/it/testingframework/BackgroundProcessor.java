package io.quarkiverse.githubapp.it.testingframework;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkiverse.githubapp.GitHubClientProvider;

@ApplicationScoped
public class BackgroundProcessor {

    public static Behavior behavior;

    @Inject
    GitHubClientProvider clientProvider;

    public void process() throws IOException {
        behavior.execute(clientProvider);
    }

    public interface Behavior {
        void execute(GitHubClientProvider clientProvider) throws IOException;
    }
}
