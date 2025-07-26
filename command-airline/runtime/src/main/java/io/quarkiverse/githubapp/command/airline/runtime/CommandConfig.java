package io.quarkiverse.githubapp.command.airline.runtime;

import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ReactionStrategy;
import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler;

public class CommandConfig {

    private final CommandScope scope;
    private final ExecutionErrorStrategy executionErrorStrategy;
    private final String executionErrorMessage;
    private final Class<? extends ExecutionErrorHandler> executionErrorHandler;
    private final ReactionStrategy reactionStrategy;

    public CommandConfig(CommandScope scope,
            ExecutionErrorStrategy executionErrorStrategy,
            String executionErrorMessage,
            Class<? extends ExecutionErrorHandler> executionErrorHandler,
            ReactionStrategy reactionStrategy) {
        this.scope = scope;
        this.executionErrorStrategy = executionErrorStrategy;
        this.executionErrorMessage = executionErrorMessage;
        this.executionErrorHandler = executionErrorHandler;
        this.reactionStrategy = reactionStrategy;
    }

    public CommandConfig() {
        this.scope = CommandOptions.DEFAULT_SCOPE;
        this.executionErrorStrategy = CommandOptions.DEFAULT_EXECUTION_ERROR_STRATEGY;
        this.executionErrorMessage = CommandOptions.DEFAULT_EXECUTION_ERROR_MESSAGE;
        this.executionErrorHandler = CommandOptions.DEFAULT_EXECUTION_ERROR_HANDLER;
        this.reactionStrategy = CommandOptions.DEFAULT_REACTION_STRATEGY;
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

    public Class<? extends ExecutionErrorHandler> getExecutionErrorHandler() {
        return executionErrorHandler;
    }

    public ReactionStrategy getReactionStrategy() {
        return reactionStrategy;
    }
}
