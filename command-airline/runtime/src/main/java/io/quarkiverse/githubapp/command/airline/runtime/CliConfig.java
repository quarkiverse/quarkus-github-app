package io.quarkiverse.githubapp.command.airline.runtime;

import java.util.List;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;

public class CliConfig {

    private final List<String> aliases;
    private final ParseErrorStrategy parseErrorStrategy;
    private final String parseErrorMessage;
    private final CommandConfig defaultCommandConfig;

    public CliConfig(List<String> aliases) {
        this.aliases = aliases;
        this.parseErrorStrategy = CliOptions.DEFAULT_PARSE_ERROR_STRATEGY;
        this.parseErrorMessage = CliOptions.DEFAULT_PARSE_ERROR_MESSAGE;
        this.defaultCommandConfig = new CommandConfig();
    }

    public CliConfig(List<String> aliases, ParseErrorStrategy parseErrorStrategy, String parseErrorMessage,
            CommandConfig defaultCommandConfig) {
        this.aliases = aliases;
        this.parseErrorStrategy = parseErrorStrategy;
        this.parseErrorMessage = parseErrorMessage;
        this.defaultCommandConfig = defaultCommandConfig;
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
}
