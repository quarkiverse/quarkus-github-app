package io.quarkiverse.githubapp.command.airline.runtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHReaction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.ReactionContent;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.builder.ParserBuilder;
import com.github.rvesse.airline.model.MetadataLoader;
import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.handlers.CollectAll;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.command.airline.AirlineInject;
import io.quarkiverse.githubapp.command.airline.CommandOptions.DefaultExecutionErrorHandlerMarker;
import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler;
import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler.ExecutionErrorContext;
import io.quarkiverse.githubapp.command.airline.ParseErrorHandler;
import io.quarkiverse.githubapp.command.airline.ParseErrorHandler.ParseErrorContext;
import io.quarkiverse.githubapp.command.airline.runtime.util.Commandline;
import io.quarkiverse.githubapp.command.airline.runtime.util.Reactions;
import io.quarkiverse.githubapp.runtime.telemetry.opentelemetry.OpenTelemetryAttributes.CommandErrorType;
import io.quarkiverse.githubapp.telemetry.TelemetryMetricsReporter;
import io.quarkiverse.githubapp.telemetry.TelemetryTracesReporter;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public abstract class AbstractCommandDispatcher<C> {

    private static final Logger LOGGER = Logger.getLogger(AbstractCommandDispatcher.class);

    private final Cli<C> cli;
    private final CliConfig cliConfig;
    private final Map<String, CommandConfig> commandConfigs;
    private final Map<String, CommandPermissionConfig> commandPermissionConfigs;
    private final Map<String, CommandTeamConfig> commandTeamConfigs;

    @Inject
    TelemetryTracesReporter openTelemetryTracesReporter;
    @Inject
    TelemetryMetricsReporter openTelemetryMetricsReporter;

    protected AbstractCommandDispatcher(Class<?> cliClass, CliConfig cliConfig) {
        ParserBuilder<C> parserBuilder = new ParserBuilder<C>();
        parserBuilder.withCommandFactory(new ArcCommandFactory<>());
        parserBuilder.withErrorHandler(new CollectAll());
        parserBuilder.withCompositionAnnotations(AirlineModule.class.getName(), AirlineInject.class.getName());

        this.cli = new Cli<>(MetadataLoader.loadGlobal(cliClass, parserBuilder.build()));
        this.cliConfig = cliConfig;
        this.commandConfigs = getCommandConfigs();
        this.commandPermissionConfigs = getCommandPermissionConfigs();
        this.commandTeamConfigs = getCommandTeamConfigs();
    }

    protected abstract Map<String, CommandConfig> getCommandConfigs();

    protected abstract Map<String, CommandPermissionConfig> getCommandPermissionConfigs();

    protected abstract Map<String, CommandTeamConfig> getCommandTeamConfigs();

    protected Optional<CommandExecutionContext<C>> getCommand(GitHubEvent gitHubEvent,
            GHEventPayload.IssueComment issueCommentPayload) {
        String body = issueCommentPayload.getComment().getBody();

        if (body == null || body.isBlank()) {
            return Optional.empty();
        }

        Optional<String> firstLineOptional = body.trim().lines().findFirst();
        if (firstLineOptional.isEmpty() || firstLineOptional.get().isBlank()) {
            return Optional.empty();
        }

        String firstLine = firstLineOptional.get().trim();
        String cliName = firstLine.split(" ", 2)[0];

        if (!matches(cliName)) {
            return Optional.empty();
        }

        List<String> commandLine;
        try {
            commandLine = Commandline.translateCommandline(firstLine);
            commandLine.remove(0);
        } catch (IllegalArgumentException e) {
            handleParseError(gitHubEvent, issueCommentPayload, firstLine, null, e.getMessage());

            if (cliConfig.getDefaultCommandConfig().getReactionStrategy().reactionOnError()) {
                Reactions.createReaction(issueCommentPayload, ReactionContent.CONFUSED);
            }

            return Optional.empty();
        }

        ParseResult<C> parseResult = cli.parseWithResult(commandLine);

        if (parseResult.wasSuccessful()) {
            String commandClassName = parseResult.getState().getCommand().getType().getName();

            CommandConfig commandConfig = commandConfigs.getOrDefault(commandClassName,
                    cliConfig.getDefaultCommandConfig());
            if (!commandConfig.getScope().isInScope(issueCommentPayload.getIssue().isPullRequest())) {
                return Optional.empty();
            }

            CommandPermissionConfig commandPermissionConfig = commandPermissionConfigs.getOrDefault(commandClassName,
                    cliConfig.getDefaultCommandPermissionConfig());
            CommandTeamConfig commandTeamConfig = commandTeamConfigs.getOrDefault(commandClassName,
                    cliConfig.getDefaultCommandTeamConfig());

            if (!hasPermission(commandPermissionConfig, commandTeamConfig, issueCommentPayload.getRepository(),
                    issueCommentPayload.getSender())) {
                if (commandConfig.getReactionStrategy().reactionOnError()) {
                    Reactions.createReaction(issueCommentPayload, ReactionContent.MINUS_ONE);
                }
                openTelemetryTracesReporter.reportCommandMethodError(gitHubEvent, commandClassName, firstLine,
                        CommandErrorType.PERMISSION_ERROR, null);
                openTelemetryMetricsReporter.incrementCommandMethodError(gitHubEvent, commandClassName,
                        CommandErrorType.PERMISSION_ERROR, null);
                return Optional.empty();
            }

            GHReaction ackReaction;
            if (commandConfig.getReactionStrategy().reactionOnProgress()) {
                ackReaction = Reactions.createReaction(issueCommentPayload, ReactionContent.ROCKET);
            } else {
                ackReaction = null;
            }

            return Optional.of(new CommandExecutionContext<>(firstLine, parseResult.getCommand(), commandConfig, ackReaction));
        }

        CommandConfig bestCommandConfig = getBestCommandConfigInErrorState(parseResult);
        if (bestCommandConfig.getReactionStrategy().reactionOnError()) {
            Reactions.createReaction(issueCommentPayload, ReactionContent.CONFUSED);
        }

        handleParseError(gitHubEvent, issueCommentPayload, firstLine, parseResult, null);

        return Optional.empty();
    }

    private boolean matches(String cli) {
        for (String alias : cliConfig.getAliases()) {
            if (alias.equals(cli)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPermission(CommandPermissionConfig commandPermissionConfig,
            CommandTeamConfig commandTeamConfig,
            GHRepository repository, GHUser user) {
        try {
            if (commandPermissionConfig.getPermission() != null) {
                if (user == null) {
                    return false;
                }

                if (!repository.hasPermission(user, commandPermissionConfig.getPermission())) {
                    return false;
                }
            }
            if (!commandTeamConfig.getTeams().isEmpty()) {
                if (user == null) {
                    return false;
                }

                List<GHTeam> matchingTeams = repository.getTeams().stream()
                        .filter(t -> commandTeamConfig.getTeams().contains(t.getSlug()))
                        .toList();

                for (GHTeam matchingTeam : matchingTeams) {
                    if (matchingTeam.hasMember(user)) {
                        return true;
                    }
                }

                return false;
            }

            return true;
        } catch (IOException e) {
            throw new CommandExecutionException("Unable to check the permissions for the command", e);
        }
    }

    private CommandConfig getBestCommandConfigInErrorState(ParseResult<C> parseResult) {
        if (parseResult.getState().getCommand() == null) {
            return cliConfig.getDefaultCommandConfig();
        }

        return commandConfigs.getOrDefault(parseResult.getState().getCommand().getType().getName(),
                cliConfig.getDefaultCommandConfig());
    }

    protected void handleParseError(GitHubEvent gitHubEvent, IssueComment issueCommentPayload, String command,
            ParseResult<C> parseResult, String error) {
        Class<? extends ParseErrorHandler> parseErrorHandlerClass = cliConfig.getParseErrorHandler();

        try (InstanceHandle<? extends ParseErrorHandler> parseErrorHandlerInstance = Arc.container()
                .instance(parseErrorHandlerClass)) {
            if (!parseErrorHandlerInstance.isAvailable()) {
                throw new IllegalStateException(
                        "Unable to find or create a bean for ParseErrorHandler class: " + parseErrorHandlerClass.getName());
            }

            parseErrorHandlerInstance.get().handleParseError(issueCommentPayload,
                    new ParseErrorContext(cliConfig, cli, command, parseResult, error));
        }

        openTelemetryTracesReporter.reportCommandMethodError(gitHubEvent, null, command, CommandErrorType.PARSE_ERROR,
                error);
        openTelemetryMetricsReporter.incrementCommandMethodError(gitHubEvent, null, CommandErrorType.PARSE_ERROR,
                error);
    }

    protected void handleExecutionError(GitHubEvent gitHubEvent, GHEventPayload.IssueComment issueCommentPayload,
            CommandExecutionContext<C> commandExecutionContext, Exception exception) {
        Class<? extends ExecutionErrorHandler> executionErrorHandlerClass = commandExecutionContext.getCommandConfig()
                .getExecutionErrorHandler();
        if (DefaultExecutionErrorHandlerMarker.class.equals(executionErrorHandlerClass)) {
            executionErrorHandlerClass = cliConfig.getDefaultCommandConfig().getExecutionErrorHandler();
            if (DefaultExecutionErrorHandlerMarker.class.equals(executionErrorHandlerClass)) {
                executionErrorHandlerClass = DefaultExecutionErrorHandler.class;
            }
        }

        try (InstanceHandle<? extends ExecutionErrorHandler> executionErrorHandlerInstance = Arc.container()
                .instance(executionErrorHandlerClass)) {
            if (!executionErrorHandlerInstance.isAvailable()) {
                throw new IllegalStateException("Unable to find or create a bean for ExecutionErrorHandler class: "
                        + executionErrorHandlerClass.getName());
            }

            executionErrorHandlerInstance.get().handleExecutionError(issueCommentPayload,
                    new ExecutionErrorContext(commandExecutionContext, exception));
        }

        openTelemetryTracesReporter.reportCommandMethodError(gitHubEvent,
                commandExecutionContext.getCommand().getClass().getName(), commandExecutionContext.getCommandLine(),
                CommandErrorType.EXECUTION_ERROR, exception.getMessage());
        openTelemetryMetricsReporter.incrementCommandMethodError(gitHubEvent,
                commandExecutionContext.getCommand().getClass().getName(), CommandErrorType.EXECUTION_ERROR,
                exception.getMessage());
    }

    protected void handleSuccess(GitHubEvent gitHubEvent, GHEventPayload.IssueComment issueCommentPayload,
            CommandExecutionContext<C> commandExecutionContext) {
        openTelemetryTracesReporter.reportCommandMethodSuccess(gitHubEvent,
                commandExecutionContext.getCommand().getClass().getName(), commandExecutionContext.getCommandLine());
        openTelemetryMetricsReporter.incrementCommandMethodSuccess(gitHubEvent,
                commandExecutionContext.getCommand().getClass().getName());
    }

    public static class CommandExecutionContext<C> {

        private final String commandLine;

        private final C command;

        private final GHReaction ackReaction;

        private final CommandConfig commandConfig;

        public CommandExecutionContext(String commandLine, C command, CommandConfig commandConfig, GHReaction ackReaction) {
            this.commandLine = commandLine;
            this.command = command;
            this.commandConfig = commandConfig;
            this.ackReaction = ackReaction;
        }

        public String getCommandLine() {
            return commandLine;
        }

        public C getCommand() {
            return command;
        }

        public CommandConfig getCommandConfig() {
            return commandConfig;
        }

        public GHReaction getAckReaction() {
            return ackReaction;
        }
    }
}
