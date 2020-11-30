package io.quarkiverse.githubapp.testing.dsl;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public interface GitHubMockContext {

    GitHub client(long id);

    GHRepository repository(String id);

    GHIssue issue(long id);

    GHPullRequest pullRequest(long id);

    Object[] ghObjects();

}
