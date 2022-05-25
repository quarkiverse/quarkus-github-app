package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CliOptionsAliasesCli.AliasesCliCommand;
import io.quarkiverse.githubapp.command.airline.CliOptions;

// tag::aliases[]
@Cli(name = "@quarkus-bot", commands = { AliasesCliCommand.class })
@CliOptions(aliases = { "@quarkusbot", "@bot" }) // <1>
public class CliOptionsAliasesCli {
    // end::aliases[]

    @Command(name = "command")
    static class AliasesCliCommand implements Runnable {

        @Override
        public void run() {
        }
    }
}