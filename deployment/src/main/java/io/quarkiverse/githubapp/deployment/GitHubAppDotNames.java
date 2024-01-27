package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.event.Event;
import io.quarkiverse.githubapp.runtime.Multiplexer;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());
    static final DotName MULTIPLEXER = DotName.createSimple(Multiplexer.class.getName());
    static final DotName ERROR_HANDLER = DotName.createSimple(ErrorHandler.class.getName());

    static final DotName GITHUB = DotName.createSimple(GitHub.class.getName());
    static final DotName GITHUB_EVENT = DotName.createSimple(GitHubEvent.class.getName());
    static final DotName DYNAMIC_GRAPHQL_CLIENT = DotName.createSimple(DynamicGraphQLClient.class.getName());

    private GitHubAppDotNames() {
    }
}
