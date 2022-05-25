package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CliOptionsDefaultCommandOptionsCli.DefaultCommandOptionsCliCommand;
import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;

// tag::default-command-options[]
@Cli(name = "@bot", commands = { DefaultCommandOptionsCliCommand.class })
@CliOptions(defaultCommandOptions = @CommandOptions(scope = CommandScope.ISSUES)) // <1>
public class CliOptionsDefaultCommandOptionsCli {
    // end::default-command-options[]

    @Command(name = "command")
    static class DefaultCommandOptionsCliCommand implements Runnable {

        @Override
        public void run() {
        }
    }
}