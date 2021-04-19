package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.Event;
import io.quarkiverse.githubapp.runtime.Multiplexer;
import io.quarkiverse.githubapp.runtime.replay.ReplayEventsRoute;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());
    static final DotName MULTIPLEXER = DotName.createSimple(Multiplexer.class.getName());
    static final DotName REPLAY_EVENTS_ROUTE = DotName.createSimple(ReplayEventsRoute.class.getName());

    private GitHubAppDotNames() {
    }
}
