package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler;
import io.quarkiverse.githubapp.it.command.airline.ExecutionErrorHandlerCli.TestCommand1;

@Cli(name = "@execution-error-handler", commands = { TestCommand1.class })
public class ExecutionErrorHandlerCli {

    @Command(name = "command1")
    @CommandOptions(executionErrorHandler = CustomExecutionErrorHandler.class)
    static class TestCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            throw new RuntimeException("Execution error");
        }
    }

    public interface TestCommand {

        void run(IssueComment issueCommentPayload) throws IOException;
    }

    @Singleton
    public static class CustomExecutionErrorHandler implements ExecutionErrorHandler {

        @Override
        public void handleExecutionError(IssueComment issueCommentPayload, ExecutionErrorContext executionErrorContext) {
            try {
                issueCommentPayload.getIssue().comment("My custom error message.");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
