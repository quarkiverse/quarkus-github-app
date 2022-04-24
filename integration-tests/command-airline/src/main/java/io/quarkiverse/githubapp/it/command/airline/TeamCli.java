package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.Team;
import io.quarkiverse.githubapp.it.command.airline.TeamCli.TestTeam1Command;
import io.quarkiverse.githubapp.it.command.airline.TeamCli.TestTwoTeamsCommand;

@Cli(name = "@team", commands = { TestTeam1Command.class, TestTwoTeamsCommand.class })
class TeamCli {

    @Command(name = "team1")
    @Team("my-team-1")
    static class TestTeam1Command implements TeamCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @team team1");
        }
    }

    @Command(name = "two-teams")
    @Team({ "my-team-1", "my-team-2" })
    static class TestTwoTeamsCommand implements TeamCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @team two-teams");
        }
    }

    public interface TeamCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
