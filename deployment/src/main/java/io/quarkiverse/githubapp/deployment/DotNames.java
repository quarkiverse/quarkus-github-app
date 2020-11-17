package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.githubapp.event.Event;

final class DotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());

    private DotNames() {
    }
}
