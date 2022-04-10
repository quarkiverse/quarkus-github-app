package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;
import java.util.List;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Group;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.it.command.airline.DefaultCli.TestBasicCommand;
import io.quarkiverse.githubapp.it.command.airline.DefaultCli.TestCommandWithArguments;
import io.quarkiverse.githubapp.it.command.airline.DefaultCli.TestGroup1Command1;
import io.quarkiverse.githubapp.it.command.airline.DefaultCli.TestGroup1Command2;

@Cli(name = "@default", description = "A default command to test various features", commands = { TestBasicCommand.class,
        TestCommandWithArguments.class }, groups = {
                @Group(name = "group1", commands = { TestGroup1Command1.class, TestGroup1Command2.class }) })
@CliOptions(aliases = "@other-alias")
class DefaultCli {

    @Command(name = "basic")
    static class TestBasicCommand implements DefaultCommand {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default basic");
        }
    }

    @Command(name = "command-with-arguments")
    static class TestCommandWithArguments implements DefaultCommand {

        @Arguments(title = "username")
        private List<String> arguments;

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default command-with-arguments " + String.join(",", arguments));
        }
    }

    @Command(name = "command1")
    static class TestGroup1Command1 implements DefaultCommand {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default group1 command1");
        }
    }

    @Command(name = "command2")
    static class TestGroup1Command2 implements DefaultCommand {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default group1 command2");
        }
    }

    public interface DefaultCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
