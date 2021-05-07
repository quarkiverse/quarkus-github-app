package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.event.Event;
import io.quarkiverse.githubapp.runtime.Multiplexer;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());
    static final DotName MULTIPLEXER = DotName.createSimple(Multiplexer.class.getName());
    static final DotName ERROR_HANDLER = DotName.createSimple(ErrorHandler.class.getName());

    private GitHubAppDotNames() {
    }
}
