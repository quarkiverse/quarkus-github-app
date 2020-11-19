package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.githubapp.event.Event;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());

    private GitHubAppDotNames() {
    }
}
