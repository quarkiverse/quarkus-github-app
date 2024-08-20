package io.quarkiverse.githubapp.testing.internal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.kohsuke.github.GHEvent;

import io.quarkiverse.githubapp.runtime.Headers;
import io.quarkiverse.githubapp.runtime.Routes;
import io.quarkiverse.githubapp.testing.dsl.EventSenderOptions;
import io.quarkus.arc.Arc;
import io.vertx.core.json.JsonObject;

final class EventSenderOptionsImpl implements EventSenderOptions {

    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    private final GitHubAppTestingContext testingContext;
    private final HttpClient httpClient;

    private UUID requestId;
    private UUID deliveryId;
    private Long installationId;
    private String payload;
    private String contentType;

    EventSenderOptionsImpl(GitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
        Duration forever = Duration.ofDays(1);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(forever)
                .build();
    }

    @Override
    public EventSenderOptions requestId(UUID requestId) {
        this.requestId = requestId;
        return this;
    }

    @Override
    public EventSenderOptions deliveryId(UUID deliveryId) {
        this.deliveryId = deliveryId;
        return this;
    }

    @Override
    public EventSenderOptions payloadFromString(String payload) {
        return payloadFromString(payload, DEFAULT_CONTENT_TYPE);
    }

    @Override
    public EventSenderOptions payloadFromString(String payload, String contentType) {
        this.payload = payload;
        this.contentType = contentType;

        JsonObject payloadAsJsonObject = new JsonObject(payload);
        installationId = Arc.container().instance(Routes.class).get().extractInstallationId(payloadAsJsonObject);

        return this;
    }

    @Override
    public EventSenderOptions payloadFromClasspath(String path) throws IOException {
        return payloadFromClasspath(path, DEFAULT_CONTENT_TYPE);
    }

    @Override
    public EventSenderOptions payloadFromClasspath(String path, String contentType) throws IOException {
        return payloadFromString(testingContext.getFromClasspath(path), contentType);
    }

    @Override
    public EventHandlingResponseImpl event(GHEvent event) {
        return event(event, false);
    }

    @Override
    public EventHandlingResponseImpl event(GHEvent event, boolean ignoreExceptions) {
        HttpRequest request = HttpRequest.newBuilder(buildUrl())
                .POST(BodyPublishers.ofString(payload))
                .header(Headers.CONTENT_TYPE, contentType)
                .header(Headers.X_REQUEST_ID, (requestId == null ? UUID.randomUUID() : requestId).toString())
                .header(Headers.X_GITHUB_DELIVERY, (deliveryId == null ? UUID.randomUUID() : deliveryId).toString())
                .header(Headers.X_GITHUB_EVENT, symbol(event))
                .build();

        // Only stub these methods when we know they are going to get called;
        // otherwise tests might throw a UnnecessaryStubbingException when using Mockito's "strict-stubs" mode
        // and testing background processing instead of events.
        testingContext.mocks.initEventStubs(installationId);

        testingContext.errorHandler.captured = null;
        AssertionError callAssertionError = null;
        try {
            httpClient.send(request, BodyHandlers.discarding());
        } catch (Throwable e) {
            callAssertionError = new AssertionError("The HTTP call threw an exception: " + e.getMessage(), e);
        }
        AssertionError handlingAssertionError = null;
        if (testingContext.errorHandler.captured != null) {
            // For some reason quarkus-github-app wraps the exceptions in CompletionException.
            // Unwrap the exceptions, as it's not what users expect.
            Throwable unwrappedCaptured = unwrapCompletionException(testingContext.errorHandler.captured);
            handlingAssertionError = new AssertionError("The event handler threw an exception: "
                    + unwrappedCaptured.getMessage(),
                    unwrappedCaptured);
        }
        if (handlingAssertionError != null) {
            if (callAssertionError != null) {
                handlingAssertionError.addSuppressed(callAssertionError);
            }
            if (!ignoreExceptions) {
                throw handlingAssertionError;
            }
        } else if (callAssertionError != null) {
            throw callAssertionError;
        }
        return new EventHandlingResponseImpl(testingContext);
    }

    private static Throwable unwrapCompletionException(Throwable captured) {
        if (captured instanceof CompletionException && captured.getCause() != null) {
            return captured.getCause();
        } else {
            return captured;
        }
    }

    private URI buildUrl() {
        int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class)
                .orElse(8081);
        String path = "/";
        return URI.create("http://localhost:" + port + path);
    }

    private String symbol(GHEvent event) {
        if (event == GHEvent.ALL) {
            return "*";
        }
        return event.name().toLowerCase(Locale.ENGLISH);
    }
}
