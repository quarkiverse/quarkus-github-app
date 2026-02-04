package ilove.quark.us

import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Cli
import com.github.rvesse.airline.annotations.Command
import io.quarkiverse.githubapp.command.airline.AbstractHelpCommand
import java.io.IOException
import org.kohsuke.github.GHEventPayload

// TODO: make sure you adjust the name as @bot is an actual GitHub user
@Cli(
    name = "@bot",
    commands = [MyGitHubBot.SayHello::class, MyGitHubBot.Help::class],
    description = "A friendly bot",
)
class MyGitHubBot {
    
    interface Commands {
        @Throws(IOException::class)
        fun run(issueCommentPayload: GHEventPayload.IssueComment)
    }
    
    @Command(name = "say-hello", description = "Says hello")
    class SayHello : Commands {
        
        @Arguments var arguments: MutableList<String> = arrayListOf()
        
        @Throws(IOException::class)
        override fun run(issueCommentPayload: GHEventPayload.IssueComment) {
            issueCommentPayload.issue.comment(":wave: Hello " + arguments.joinToString(" "))
        }
    }
    
    @Command(name = "help", description = "Displays help")
    class Help : AbstractHelpCommand(), Commands {
        @Throws(IOException::class)
        override fun run(issueCommentPayload: GHEventPayload.IssueComment) {
            super.run(issueCommentPayload)
        }
    }
}
