package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHPermissionType;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.PermissionOverrideCli.AdminCommand;
import command.airline.PermissionOverrideCli.WriteCommand;
import io.quarkiverse.githubapp.command.airline.Permission;

// tag::include[]
@Cli(name = "@bot", commands = { WriteCommand.class, AdminCommand.class })
@Permission(GHPermissionType.WRITE) // <1>
public class PermissionOverrideCli {

    interface Commands {

        void run() throws IOException;
    }

    @Command(name = "write-command") // <2>
    static class WriteCommand implements Commands {

        @Override
        public void run() throws IOException {
            // do something
        }
    }

    @Command(name = "admin-command")
    @Permission(GHPermissionType.ADMIN) // <3>
    static class AdminCommand implements Commands {

        @Override
        public void run() throws IOException {
            // do something
        }
    }
}
// end::include[]
