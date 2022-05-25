package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CliOptionsParseErrorStrategyCli.ParseErrorStrategyCliCommand;
import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;

// tag::parse-error-strategy[]
@Cli(name = "@bot", commands = { ParseErrorStrategyCliCommand.class })
@CliOptions(parseErrorStrategy = ParseErrorStrategy.NONE) // <1>
public class CliOptionsParseErrorStrategyCli {
    // end::parse-error-strategy[]

    @Command(name = "command")
    static class ParseErrorStrategyCliCommand implements Runnable {

        @Override
        public void run() {
        }
    }
}
