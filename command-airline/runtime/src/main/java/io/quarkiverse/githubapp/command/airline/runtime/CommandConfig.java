package io.quarkiverse.githubapp.command.airline.runtime;

import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;

public class CommandConfig {

    private final CommandScope scope;
    private final ExecutionErrorStrategy executionErrorStrategy;
    private final String executionErrorMessage;

    public CommandConfig(CommandScope scope, ExecutionErrorStrategy executionErrorStrategy, String executionErrorMessage) {
        this.scope = scope;
        this.executionErrorStrategy = executionErrorStrategy;
        this.executionErrorMessage = executionErrorMessage;
    }

    public CommandConfig() {
        this.scope = CommandOptions.DEFAULT_SCOPE;
        this.executionErrorStrategy = CommandOptions.DEFAULT_EXECUTION_ERROR_STRATEGY;
        this.executionErrorMessage = CommandOptions.DEFAULT_EXECUTION_ERROR_MESSAGE;
    }

    public CommandScope getScope() {
        return scope;
    }

    public ExecutionErrorStrategy getExecutionErrorStrategy() {
        return executionErrorStrategy;
    }

    public String getExecutionErrorMessage() {
        return executionErrorMessage;
    }
}
