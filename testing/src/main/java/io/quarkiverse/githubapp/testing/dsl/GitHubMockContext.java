package io.quarkiverse.githubapp.testing.dsl;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHObject;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public interface GitHubMockContext {

    GitHub client(long id);

    DynamicGraphQLClient graphQLClient(long id);

    GHRepository repository(String id);

    GHIssue issue(long id);

    GHPullRequest pullRequest(long id);

    GHIssueComment issueComment(long id);

    GHTeam team(long id);

    <T extends GHObject> T ghObject(Class<T> type, long id);

    Object[] ghObjects();

}
