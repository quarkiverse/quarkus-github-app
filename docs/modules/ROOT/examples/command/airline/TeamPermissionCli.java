package command.airline;

import java.io.IOException;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.TeamPermissionCli.MyTeamCommand;
import io.quarkiverse.githubapp.command.airline.Team;

// tag::include[]
@Cli(name = "@bot", commands = { MyTeamCommand.class })
public class TeamPermissionCli {

    interface Commands {

        void run() throws IOException;
    }

    @Command(name = "command")
    @Team({ "my-team1", "my-team2" }) // <1>
    static class MyTeamCommand implements Commands {

        @Override
        public void run() throws IOException {
            // do something
        }
    }
}
// end::include[]
