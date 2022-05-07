package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHPermissionType;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.PermissionCli.WriteCommand;
import io.quarkiverse.githubapp.command.airline.Permission;

// tag::include[]
@Cli(name = "@bot", commands = { WriteCommand.class })
public class PermissionCli {

    interface Commands {

        void run() throws IOException;
    }

    @Command(name = "write-command")
    @Permission(GHPermissionType.WRITE) // <1>
    static class WriteCommand implements Commands {

        @Override
        public void run() throws IOException {
            // do something
        }
    }
}
// end::include[]
