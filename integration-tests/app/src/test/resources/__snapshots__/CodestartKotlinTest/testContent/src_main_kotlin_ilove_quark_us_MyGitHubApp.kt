package ilove.quark.us

import java.io.IOException
import org.kohsuke.github.GHEventPayload
import io.quarkiverse.githubapp.event.Issue

open class MyGitHubApp {
  
  @Throws(IOException::class)
  fun onOpen(@Issue.Opened issuePayload: GHEventPayload.Issue) {
    issuePayload.issue.comment(":wave: Hello from my GitHub App")
  }
}
