package io.quarkiverse.githubapp.command.airline.runtime;

import java.util.List;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;
import io.quarkiverse.githubapp.command.airline.ParseErrorHandler;

public class CliConfig {

    private final List<String> aliases;
    private final ParseErrorStrategy parseErrorStrategy;
    private final String parseErrorMessage;
    private final Class<? extends ParseErrorHandler> parseErrorHandler;
    private final CommandConfig defaultCommandConfig;
    private final CommandPermissionConfig defaultCommandPermissionConfig;
    private final CommandTeamConfig defaultCommandTeamConfig;

    public CliConfig(List<String> aliases, CommandConfig defaultCommandConfig,
            CommandPermissionConfig defaultCommandPermissionConfig,
            CommandTeamConfig defaultCommandTeamConfig) {
        this.aliases = aliases;
        this.parseErrorStrategy = CliOptions.DEFAULT_PARSE_ERROR_STRATEGY;
        this.parseErrorMessage = CliOptions.DEFAULT_PARSE_ERROR_MESSAGE;
        this.parseErrorHandler = CliOptions.DEFAULT_PARSE_ERROR_HANDLER;
        this.defaultCommandConfig = defaultCommandConfig;
        this.defaultCommandPermissionConfig = defaultCommandPermissionConfig;
        this.defaultCommandTeamConfig = defaultCommandTeamConfig;
    }

    public CliConfig(List<String> aliases, ParseErrorStrategy parseErrorStrategy, String parseErrorMessage,
            Class<? extends ParseErrorHandler> parseErrorHandler,
            CommandConfig defaultCommandConfig, CommandPermissionConfig defaultCommandPermissionConfig,
            CommandTeamConfig defaultCommandTeamConfig) {
        this.aliases = aliases;
        this.parseErrorStrategy = parseErrorStrategy;
        this.parseErrorMessage = parseErrorMessage;
        this.parseErrorHandler = parseErrorHandler;
        this.defaultCommandConfig = defaultCommandConfig;
        this.defaultCommandPermissionConfig = defaultCommandPermissionConfig;
        this.defaultCommandTeamConfig = defaultCommandTeamConfig;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public ParseErrorStrategy getParseErrorStrategy() {
        return parseErrorStrategy;
    }

    public String getParseErrorMessage() {
        return parseErrorMessage;
    }

    public Class<? extends ParseErrorHandler> getParseErrorHandler() {
        return parseErrorHandler;
    }

    public CommandConfig getDefaultCommandConfig() {
        return defaultCommandConfig;
    }

    public CommandPermissionConfig getDefaultCommandPermissionConfig() {
        return defaultCommandPermissionConfig;
    }

    public CommandTeamConfig getDefaultCommandTeamConfig() {
        return defaultCommandTeamConfig;
    }
}
