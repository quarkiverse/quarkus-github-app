package io.quarkiverse.githubapp.testing.dsl;

import java.io.IOException;
import java.util.UUID;

import org.kohsuke.github.GHEvent;

public interface EventSenderOptions {
    EventSenderOptions requestId(UUID requestId);

    EventSenderOptions deliveryId(UUID deliveryId);

    EventSenderOptions payloadFromString(String payload);

    EventSenderOptions payloadFromString(String payload, String contentType);

    EventSenderOptions payloadFromClasspath(String path) throws IOException;

    EventSenderOptions payloadFromClasspath(String path, String contentType) throws IOException;

    EventHandlingResponse event(GHEvent event);

    EventHandlingResponse event(GHEvent event, boolean ignoreExceptions);

    EventHandlingResponse rawEvent(String event);

    EventHandlingResponse rawEvent(String event, boolean ignoreExceptions);
}
