package io.quarkiverse.githubapp.command.airline.runtime;

public class CommandExecutionException extends RuntimeException {

    public CommandExecutionException(String message, Exception cause) {
        super(message, cause);
    }
}
