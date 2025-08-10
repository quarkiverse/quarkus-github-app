package io.quarkiverse.githubapp.runtime.error;

public class GitHubEventDispatchingException extends RuntimeException {

    GitHubEventDispatchingException(String message, Throwable e) {
        super(message, e);
    }
}
