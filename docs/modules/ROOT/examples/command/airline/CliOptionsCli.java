package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CliOptionsCli.AliasesCli.AliasesCliCommand;
import command.airline.CliOptionsCli.DefaultCommandOptionsCli.DefaultCommandOptionsCliCommand;
import command.airline.CliOptionsCli.ParseErrorMessageCli.ParseErrorMessageCliCommand;
import command.airline.CliOptionsCli.ParseErrorStrategyCli.ParseErrorStrategyCliCommand;
import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;

public class CliOptionsCli {
    // tag::aliases[]
    @Cli(name = "@quarkus-bot", commands = { AliasesCliCommand.class })
    @CliOptions(aliases = { "@quarkusbot", "@bot" }) // <1>
    public class AliasesCli {
        // end::aliases[]

        @Command(name = "command")
        class AliasesCliCommand implements Runnable {

            @Override
            public void run() {
            }
        }
    }

    // tag::default-command-options[]
    @Cli(name = "@bot", commands = { DefaultCommandOptionsCliCommand.class })
    @CliOptions(defaultCommandOptions = @CommandOptions(scope = CommandScope.ISSUES)) // <1>
    public class DefaultCommandOptionsCli {
        // end::default-command-options[]

        @Command(name = "command")
        class DefaultCommandOptionsCliCommand implements Runnable {

            @Override
            public void run() {
            }
        }
    }

    // tag::parse-error-strategy[]
    @Cli(name = "@bot", commands = { ParseErrorStrategyCliCommand.class })
    @CliOptions(parseErrorStrategy = ParseErrorStrategy.NONE) // <1>
    public class ParseErrorStrategyCli {
        // end::parse-error-strategy[]

        @Command(name = "command")
        class ParseErrorStrategyCliCommand implements Runnable {

            @Override
            public void run() {
            }
        }
    }

    // tag::parse-error-message[]
    @Cli(name = "@bot", commands = { ParseErrorMessageCliCommand.class })
    @CliOptions(parseErrorStrategy = ParseErrorStrategy.COMMENT_MESSAGE, parseErrorMessage = "Your custom message") // <1>
    public class ParseErrorMessageCli {
        // end::parse-error-message[]

        @Command(name = "command")
        class ParseErrorMessageCliCommand implements Runnable {

            @Override
            public void run() {
            }
        }
    }
}
