package ilove.quark.us;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.AbstractHelpCommand;

// TODO: make sure you adjust the name as @bot is an actual GitHub user
@Cli(name = "@bot", commands = { MyGitHubBot.SayHello.class, MyGitHubBot.Help.class }, description = "A friendly bot")
public class MyGitHubBot {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    @Command(name = "say-hello", description = "Says hello")
    static class SayHello implements Commands {

        @Arguments
        List<String> arguments = new ArrayList<>();

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue()
                    .comment(":wave: Hello " + String.join(" ", arguments));
        }
    }

    @Command(name = "help", description = "Displays help")
    static class Help extends AbstractHelpCommand implements Commands {
    }
}
