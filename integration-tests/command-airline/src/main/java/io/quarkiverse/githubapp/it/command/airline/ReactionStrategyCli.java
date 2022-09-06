package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ReactionStrategy;
import io.quarkiverse.githubapp.it.command.airline.ReactionStrategyCli.TestAllCommand;
import io.quarkiverse.githubapp.it.command.airline.ReactionStrategyCli.TestNoneCommand;
import io.quarkiverse.githubapp.it.command.airline.ReactionStrategyCli.TestProgressCommand;
import io.quarkiverse.githubapp.it.command.airline.ReactionStrategyCli.TestProgressErrorCommand;

@Cli(name = "@reaction-strategy", commands = { TestNoneCommand.class, TestProgressCommand.class, TestProgressErrorCommand.class,
        TestAllCommand.class })
public class ReactionStrategyCli {

    @Command(name = "none")
    @CommandOptions(reactionStrategy = ReactionStrategy.NONE)
    static class TestNoneCommand implements DefaultCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @reaction-strategy none");
        }
    }

    @Command(name = "progress")
    @CommandOptions(reactionStrategy = ReactionStrategy.ON_PROGRESS)
    static class TestProgressCommand implements DefaultCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @reaction-strategy progress");
        }
    }

    @Command(name = "progress-error")
    @CommandOptions(reactionStrategy = ReactionStrategy.ON_PROGRESS_ON_ERROR)
    static class TestProgressErrorCommand implements DefaultCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @reaction-strategy progress-error");
        }
    }

    @Command(name = "all")
    @CommandOptions(reactionStrategy = ReactionStrategy.ALL)
    static class TestAllCommand implements DefaultCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @reaction-strategy all");
        }
    }

    public interface DefaultCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
