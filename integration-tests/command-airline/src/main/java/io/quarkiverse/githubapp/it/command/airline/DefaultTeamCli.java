package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.Team;
import io.quarkiverse.githubapp.it.command.airline.DefaultTeamCli.TestNoTeamsCommand;
import io.quarkiverse.githubapp.it.command.airline.DefaultTeamCli.TestTeam2Command;

@Cli(name = "@default-team", commands = { TestNoTeamsCommand.class, TestTeam2Command.class })
@Team("my-team-1")
class DefaultTeamCli {

    @Command(name = "no-teams")
    static class TestNoTeamsCommand implements TeamCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-team no-teams");
        }
    }

    @Command(name = "team2")
    @Team("my-team-2")
    static class TestTeam2Command implements TeamCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-team team2");
        }
    }

    public interface TeamCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
