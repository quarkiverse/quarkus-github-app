package io.quarkiverse.githubapp.testing.dsl;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHObject;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.GitHubClientProvider;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public interface GitHubMockContext {

    /**
     * @return The mock for the application client.
     * @see GitHubClientProvider#getApplicationClient()
     */
    GitHub applicationClient();

    /**
     * @param installationId The identifier of the GitHub app installation.
     * @return The mock for the installation client.
     * @see GitHubClientProvider#getInstallationClient(long)
     */
    GitHub client(long installationId);

    /**
     * @param installationId The identifier of the GitHub app installation.
     * @return The mock for the installation GraphQL client.
     * @see GitHubClientProvider#getInstallationGraphQLClient(long)
     */
    DynamicGraphQLClient graphQLClient(long installationId);

    GHRepository repository(String id);

    GHIssue issue(long id);

    GHPullRequest pullRequest(long id);

    GHIssueComment issueComment(long id);

    GHTeam team(long id);

    <T extends GHObject> T ghObject(Class<T> type, long id);

    Object[] ghObjects();

}
