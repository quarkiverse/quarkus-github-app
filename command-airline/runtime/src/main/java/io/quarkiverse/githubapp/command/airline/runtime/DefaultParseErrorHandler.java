package io.quarkiverse.githubapp.command.airline.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.help.Help;
import com.github.rvesse.airline.parser.errors.ParseException;

import io.quarkiverse.githubapp.command.airline.ParseErrorHandler;

@Singleton
public class DefaultParseErrorHandler implements ParseErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(DefaultParseErrorHandler.class);

    @Override
    public void handleParseError(GHEventPayload.IssueComment issueCommentPayload, ParseErrorContext parseErrorContext) {
        if (!parseErrorContext.cliConfig().getParseErrorStrategy().addMessage()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format(parseErrorContext.cliConfig().getParseErrorMessage(), parseErrorContext.command()));

        if (parseErrorContext.cliConfig().getParseErrorStrategy().includeErrors()) {
            message.append("\n\nErrors:\n");
            if (parseErrorContext.error() != null) {
                message.append("\n- " + parseErrorContext.error());
            }
            if (parseErrorContext.parseResult() != null) {
                for (ParseException parseError : parseErrorContext.parseResult().getErrors()) {
                    message.append("\n- " + parseError.getMessage());
                }
            }
        }

        if (parseErrorContext.error() == null && parseErrorContext.cliConfig().getParseErrorStrategy().includeHelp()) {
            try {
                ByteArrayOutputStream helpOs = new ByteArrayOutputStream();

                if (parseErrorContext.parseResult() != null
                        && parseErrorContext.parseResult().getState().getCommand() != null) {
                    Help.help(parseErrorContext.parseResult().getState().getCommand(), helpOs);
                } else {
                    Help.help(parseErrorContext.cli().getMetadata(), Collections.emptyList(), helpOs);
                }

                String help = helpOs.toString(StandardCharsets.UTF_8);

                if (!help.isBlank()) {
                    message.append("\n\nHelp:\n\n").append("```\n" + help.trim() + "\n```");
                }
            } catch (IOException e) {
                LOGGER.warn("Error trying to generate help for parseErrorContext.command() `" + parseErrorContext.command()
                        + "` in "
                        + issueCommentPayload.getRepository().getFullName() + "#" + issueCommentPayload.getIssue().getNumber(),
                        e);
            }
        }

        try {
            issueCommentPayload.getIssue().comment(message.toString());
        } catch (Exception e) {
            LOGGER.warn(
                    "Error trying to add parseErrorContext.command() parse parseErrorContext.error() comment for parseErrorContext.command() `"
                            + parseErrorContext.command() + "` in "
                            + issueCommentPayload.getRepository().getFullName() + "#"
                            + issueCommentPayload.getIssue().getNumber(),
                    e);
        }
    }
}
