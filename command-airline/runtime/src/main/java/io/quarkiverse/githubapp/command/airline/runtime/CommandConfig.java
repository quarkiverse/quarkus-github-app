package io.quarkiverse.githubapp.command.airline.runtime;

import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ReactionStrategy;

public class CommandConfig {

    private final CommandScope scope;
    private final ExecutionErrorStrategy executionErrorStrategy;
    private final String executionErrorMessage;
    private final ReactionStrategy reactionStrategy;

    public CommandConfig(CommandScope scope, ExecutionErrorStrategy executionErrorStrategy, String executionErrorMessage,
            ReactionStrategy reactionStrategy) {
        this.scope = scope;
        this.executionErrorStrategy = executionErrorStrategy;
        this.executionErrorMessage = executionErrorMessage;
        this.reactionStrategy = reactionStrategy;
    }

    public CommandConfig() {
        this.scope = CommandOptions.DEFAULT_SCOPE;
        this.executionErrorStrategy = CommandOptions.DEFAULT_EXECUTION_ERROR_STRATEGY;
        this.executionErrorMessage = CommandOptions.DEFAULT_EXECUTION_ERROR_MESSAGE;
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

    public ReactionStrategy getReactionStrategy() {
        return reactionStrategy;
    }
}
