package io.quarkiverse.githubapp.command.airline.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHReaction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.ReactionContent;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.ParserBuilder;
import com.github.rvesse.airline.help.Help;
import com.github.rvesse.airline.model.MetadataLoader;
import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.ParseException;
import com.github.rvesse.airline.parser.errors.handlers.CollectAll;

import io.quarkiverse.githubapp.command.airline.runtime.util.Commandline;
import io.quarkiverse.githubapp.command.airline.runtime.util.Reactions;

public abstract class AbstractCommandDispatcher<C> {

    private static final Logger LOGGER = Logger.getLogger(AbstractCommandDispatcher.class);

    private final Cli<C> cli;
    private final CliConfig cliConfig;
    private final Map<String, CommandConfig> commandConfigs;
    private final Map<String, CommandPermissionConfig> commandPermissionConfigs;
    private final Map<String, CommandTeamConfig> commandTeamConfigs;

    protected AbstractCommandDispatcher(Class<?> cliClass, CliConfig cliConfig) {
        ParserBuilder<C> parserBuilder = new ParserBuilder<C>();
        parserBuilder.withCommandFactory(new ArcCommandFactory<>());
        parserBuilder.withErrorHandler(new CollectAll());

        this.cli = new Cli<>(MetadataLoader.loadGlobal(cliClass, parserBuilder.build()));
        this.cliConfig = cliConfig;
        this.commandConfigs = getCommandConfigs();
        this.commandPermissionConfigs = getCommandPermissionConfigs();
        this.commandTeamConfigs = getCommandTeamConfigs();
    }

    protected abstract Map<String, CommandConfig> getCommandConfigs();

    protected abstract Map<String, CommandPermissionConfig> getCommandPermissionConfigs();

    protected abstract Map<String, CommandTeamConfig> getCommandTeamConfigs();

    protected Optional<CommandExecutionContext<C>> getCommand(GHEventPayload.IssueComment issueCommentPayload) {
        String body = issueCommentPayload.getComment().getBody();

        if (body == null || body.isBlank()) {
            return Optional.empty();
        }

        Optional<String> firstLineOptional = body.trim().lines().findFirst();
        if (firstLineOptional.isEmpty() || firstLineOptional.get().isBlank()) {
            return Optional.empty();
        }

        String firstLine = firstLineOptional.get().trim();
        List<String> commandLine = Commandline.translateCommandline(firstLine);
        String cliName = commandLine.remove(0);

        if (!matches(cliName)) {
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

        handleParseError(issueCommentPayload, firstLine, commandLine, parseResult);

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
                        .collect(Collectors.toList());

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

    private void handleParseError(IssueComment issueCommentPayload, String command, List<String> commandLine,
            ParseResult<C> parseResult) {
        if (!cliConfig.getParseErrorStrategy().addMessage()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format(cliConfig.getParseErrorMessage(), command));

        if (cliConfig.getParseErrorStrategy().includeErrors()) {
            message.append("\n\nErrors:\n\n");
            for (ParseException error : parseResult.getErrors()) {
                message.append("- " + error.getMessage());
            }
        }

        if (cliConfig.getParseErrorStrategy().includeHelp()) {
            try {
                ByteArrayOutputStream helpOs = new ByteArrayOutputStream();

                if (parseResult.getState().getCommand() != null) {
                    Help.help(parseResult.getState().getCommand(), helpOs);
                } else {
                    Help.help(cli.getMetadata(), Collections.emptyList(), helpOs);
                }

                String help = helpOs.toString(StandardCharsets.UTF_8);

                if (!help.isBlank()) {
                    message.append("\n\nHelp:\n\n").append("```\n" + help.trim() + "\n```");
                }
            } catch (IOException e) {
                LOGGER.warn("Error trying to generate help for command `" + command + "` in "
                        + issueCommentPayload.getRepository().getFullName() + "#" + issueCommentPayload.getIssue().getNumber(),
                        e);
            }
        }

        try {
            issueCommentPayload.getIssue().comment(message.toString());
        } catch (Exception e) {
            LOGGER.warn("Error trying to add command parse error comment for command `" + command + "` in "
                    + issueCommentPayload.getRepository().getFullName() + "#" + issueCommentPayload.getIssue().getNumber(), e);
        }
    }

    protected void handleExecutionError(GHEventPayload.IssueComment issueCommentPayload,
            CommandExecutionContext<C> commandExecutionContext) {
        CommandConfig commandConfig = commandExecutionContext.getCommandConfig();
        String commandLine = commandExecutionContext.getCommandLine();

        if (!commandConfig.getExecutionErrorStrategy().addMessage()) {
            return;
        }
        try {
            issueCommentPayload.getIssue()
                    .comment(String.format(commandConfig.getExecutionErrorMessage(), commandLine));
        } catch (Exception e) {
            LOGGER.warn("Error trying to add command execution error comment for command `" + commandLine + "` in "
                    + issueCommentPayload.getRepository().getFullName() + "#" + issueCommentPayload.getIssue().getNumber(), e);
        }
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
