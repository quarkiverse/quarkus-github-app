package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.it.command.airline.DefaultCommandOptionsCli.TestCommand1;
import io.quarkiverse.githubapp.it.command.airline.DefaultCommandOptionsCli.TestCommand2;

@Cli(name = "@default-command-options", commands = { TestCommand1.class, TestCommand2.class })
@CliOptions(defaultCommandOptions = @CommandOptions(scope = CommandScope.ISSUES))
class DefaultCommandOptionsCli {

    @Command(name = "command1")
    static class TestCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-command-options command1");
        }
    }

    @Command(name = "command2")
    @CommandOptions(scope = CommandScope.ISSUES_AND_PULL_REQUESTS)
    static class TestCommand2 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-command-options command2");
        }
    }

    public interface TestCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
