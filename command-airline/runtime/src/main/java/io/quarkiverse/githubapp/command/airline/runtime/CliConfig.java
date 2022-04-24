package io.quarkiverse.githubapp.command.airline.runtime;

import java.util.List;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;

public class CliConfig {

    private final List<String> aliases;
    private final ParseErrorStrategy parseErrorStrategy;
    private final String parseErrorMessage;
    private final CommandConfig defaultCommandConfig;
    private final CommandPermissionConfig defaultCommandPermissionConfig;
    private final CommandTeamConfig defaultCommandTeamConfig;

    public CliConfig(List<String> aliases, CommandConfig defaultCommandConfig,
            CommandPermissionConfig defaultCommandPermissionConfig,
            CommandTeamConfig defaultCommandTeamConfig) {
        this.aliases = aliases;
        this.parseErrorStrategy = CliOptions.DEFAULT_PARSE_ERROR_STRATEGY;
        this.parseErrorMessage = CliOptions.DEFAULT_PARSE_ERROR_MESSAGE;
        this.defaultCommandConfig = defaultCommandConfig;
        this.defaultCommandPermissionConfig = defaultCommandPermissionConfig;
        this.defaultCommandTeamConfig = defaultCommandTeamConfig;
    }

    public CliConfig(List<String> aliases, ParseErrorStrategy parseErrorStrategy, String parseErrorMessage,
            CommandConfig defaultCommandConfig, CommandPermissionConfig defaultCommandPermissionConfig,
            CommandTeamConfig defaultCommandTeamConfig) {
        this.aliases = aliases;
        this.parseErrorStrategy = parseErrorStrategy;
        this.parseErrorMessage = parseErrorMessage;
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
