package command.airline;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.MyFirstCli.RetestCommand;

// tag::include[]
@Cli(name = "@bot", commands = { RetestCommand.class }) // <1>
public class MyFirstCli {

    @Command(name = "retest") // <2>
    static class RetestCommand implements Runnable { // <3>

        @Override
        public void run() { // <4>
            // do something
        }
    }
}
// end::include[]
