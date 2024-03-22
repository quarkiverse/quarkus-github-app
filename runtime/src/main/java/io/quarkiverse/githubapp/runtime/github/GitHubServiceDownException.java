package io.quarkiverse.githubapp.runtime.github;

public class GitHubServiceDownException extends RuntimeException {

    public GitHubServiceDownException(String message) {
        super(message);
    }
}
