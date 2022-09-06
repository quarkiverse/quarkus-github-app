package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ReactionStrategy;
import io.quarkiverse.githubapp.it.command.airline.ReactionStrategyOverrideCli.TestNoOverrideCommand;
import io.quarkiverse.githubapp.it.command.airline.ReactionStrategyOverrideCli.TestOverrideCommand;

@Cli(name = "@reaction-strategy-override", commands = { TestNoOverrideCommand.class, TestOverrideCommand.class })
@CliOptions(defaultCommandOptions = @CommandOptions(reactionStrategy = ReactionStrategy.NONE))
public class ReactionStrategyOverrideCli {

    @Command(name = "no-override")
    static class TestNoOverrideCommand implements DefaultCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @reaction-strategy-override no-override");
        }
    }

    @Command(name = "override")
    @CommandOptions(reactionStrategy = ReactionStrategy.ALL)
    static class TestOverrideCommand implements DefaultCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @reaction-strategy-override override");
        }
    }

    public interface DefaultCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
