package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;
import io.quarkiverse.githubapp.it.command.airline.ExecutionErrorStrategyCli.TestCommand1;

@Cli(name = "@execution-error-strategy", commands = { TestCommand1.class })
public class ExecutionErrorStrategyCli {

    @Command(name = "command1")
    @CommandOptions(executionErrorStrategy = ExecutionErrorStrategy.COMMENT_MESSAGE)
    static class TestCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            throw new RuntimeException("Execution error");
        }
    }

    public interface TestCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
