package io.quarkiverse.githubapp.runtime.config;

public class GitHubAppConfigurationException extends RuntimeException {

    GitHubAppConfigurationException(String message) {
        super(message);
        setStackTrace(new StackTraceElement[0]);
    }
}
