package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CliOptionsParseErrorMessageCli.ParseErrorMessageCliCommand;
import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;

// tag::parse-error-message[]
@Cli(name = "@bot", commands = { ParseErrorMessageCliCommand.class })
@CliOptions(parseErrorStrategy = ParseErrorStrategy.COMMENT_MESSAGE, parseErrorMessage = "Your custom message") // <1>
public class CliOptionsParseErrorMessageCli {
    // end::parse-error-message[]

    @Command(name = "command")
    static class ParseErrorMessageCliCommand implements Runnable {

        @Override
        public void run() {
        }
    }
}