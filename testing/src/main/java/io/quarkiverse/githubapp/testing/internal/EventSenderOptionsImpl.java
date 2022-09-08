package io.quarkiverse.githubapp.testing.internal;

import java.io.IOException;
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
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

final class EventSenderOptionsImpl implements EventSenderOptions {

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.get("application/json");

    private final GitHubAppTestingContext testingContext;
    private final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

    private UUID requestId;
    private UUID deliveryId;
    private long installationId;
    private RequestBody requestBody;

    EventSenderOptionsImpl(GitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
        Duration forever = Duration.ofDays(1);
        // For debugging
        clientBuilder
                .readTimeout(forever)
                .callTimeout(forever)
                .writeTimeout(forever);
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
        return payloadFromString(payload, DEFAULT_MEDIA_TYPE);
    }

    @Override
    public EventSenderOptions payloadFromString(String payload, String contentType) {
        return payloadFromString(payload, MediaType.get(contentType));
    }

    private EventSenderOptionsImpl payloadFromString(String payload, MediaType contentType) {
        JsonObject payloadAsJsonObject = new JsonObject(payload);
        installationId = Arc.container().instance(Routes.class).get().extractInstallationId(payloadAsJsonObject);
        requestBody = RequestBody.create(contentType, payload);
        return this;
    }

    @Override
    public EventSenderOptionsImpl payloadFromClasspath(String path) throws IOException {
        return payloadFromClasspath(path, DEFAULT_MEDIA_TYPE);
    }

    @Override
    public EventSenderOptions payloadFromClasspath(String path, String contentType) throws IOException {
        return payloadFromClasspath(path, MediaType.get(contentType));
    }

    private EventSenderOptionsImpl payloadFromClasspath(String path, MediaType contentType) throws IOException {
        return payloadFromString(testingContext.getFromClasspath(path), contentType);
    }

    @Override
    public EventHandlingResponseImpl event(GHEvent event) {
        return event(event, false);
    }

    @Override
    public EventHandlingResponseImpl event(GHEvent event, boolean ignoreExceptions) {
        OkHttpClient httpClient = clientBuilder.build();
        Call call = httpClient.newCall(new Request.Builder()
                .url(buildUrl())
                .post(requestBody)
                .addHeader(Headers.X_REQUEST_ID, (requestId == null ? UUID.randomUUID() : requestId).toString())
                .addHeader(Headers.X_GITHUB_DELIVERY, (deliveryId == null ? UUID.randomUUID() : deliveryId).toString())
                .addHeader(Headers.X_GITHUB_EVENT, symbol(event))
                .build());

        // Only stub these methods when we know they are going to get called;
        // otherwise tests might throw a UnnecessaryStubbingException when using Mockito's "strict-stubs" mode
        // and testing background processing instead of events.
        testingContext.mocks.initEventStubs(installationId);

        testingContext.errorHandler.captured = null;
        AssertionError callAssertionError = null;
        try {
            call.execute().close();
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

    private String buildUrl() {
        int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class)
                .orElse(8081);
        String path = "/";
        return "http://localhost:" + port + path;
    }

    private String symbol(GHEvent event) {
        if (event == GHEvent.ALL) {
            return "*";
        }
        return event.name().toLowerCase(Locale.ENGLISH);
    }
}
