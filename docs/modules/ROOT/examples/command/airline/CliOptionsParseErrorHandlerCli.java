package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CliOptionsParseErrorStrategyCli.ParseErrorStrategyCliCommand;
import io.quarkiverse.githubapp.command.airline.CliOptions;

// tag::parse-error-handler-config[]
@Cli(name = "@bot", commands = { ParseErrorStrategyCliCommand.class })
@CliOptions(parseErrorHandler = CustomParseErrorHandler.class) // <1>
public class CliOptionsParseErrorHandlerCli {

    // end::parse-error-handler-config[]

    @Command(name = "command")
    static class ParseErrorStrategyCliCommand implements Runnable {

        @Override
        public void run() {
        }
    }
}
