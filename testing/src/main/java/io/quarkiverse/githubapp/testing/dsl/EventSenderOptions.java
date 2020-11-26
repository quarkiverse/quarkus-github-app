package io.quarkiverse.githubapp.testing.dsl;

import org.kohsuke.github.GHEvent;

import java.io.IOException;
import java.util.UUID;

public interface EventSenderOptions {
    EventSenderOptions requestId(UUID requestId);

    EventSenderOptions deliveryId(UUID deliveryId);

    EventSenderOptions payloadFromString(String payload);

    EventSenderOptions payloadFromString(String payload, String contentType);

    EventSenderOptions payloadFromClasspath(String path) throws IOException;

    EventSenderOptions payloadFromClasspath(String path, String contentType) throws IOException;

    EventHandlingResponse event(GHEvent event);
}
