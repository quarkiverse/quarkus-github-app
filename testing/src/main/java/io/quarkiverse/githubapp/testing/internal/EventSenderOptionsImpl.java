package io.quarkiverse.githubapp.testing.internal;

import io.quarkiverse.githubapp.runtime.Headers;
import io.quarkiverse.githubapp.testing.dsl.EventSenderOptions;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.microprofile.config.ConfigProvider;
import org.kohsuke.github.GHEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class EventSenderOptionsImpl implements EventSenderOptions {

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.get("application/json");

    private final GitHubAppTestingContext testingContext;
    private final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

    private UUID requestId;
    private UUID deliveryId;
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

    private EventSenderOptions payloadFromString(String payload, MediaType contentType) {
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
        requestBody = RequestBody.create(contentType, testingContext.getFromClasspath(path));
        return this;
    }

    @Override
    public EventHandlingResponseImpl event(GHEvent event) {
        OkHttpClient httpClient = clientBuilder.build();
        Call call = httpClient.newCall(new Request.Builder()
                .url(buildUrl())
                .post(requestBody)
                .addHeader(Headers.X_REQUEST_ID, (requestId == null ? UUID.randomUUID() : requestId).toString())
                .addHeader(Headers.X_GITHUB_DELIVERY, (deliveryId == null ? UUID.randomUUID() : deliveryId).toString())
                .addHeader(Headers.X_GITHUB_EVENT, symbol(event))
                .build());
        try {
            call.execute().close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new EventHandlingResponseImpl(testingContext);
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
