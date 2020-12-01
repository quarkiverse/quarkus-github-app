package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.Event;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());

    private GitHubAppDotNames() {
    }
}
