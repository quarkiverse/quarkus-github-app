package io.quarkiverse.githubapp.runtime.smee;

import static io.quarkiverse.githubapp.runtime.Headers.FORWARDED_HEADERS;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.MessageEvent;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@ApplicationScoped
@Startup
public class SmeeIoForwarder {

    private static final String EMPTY_MESSAGE = "{}";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final EventSource eventSource;

    @Inject
    SmeeIoForwarder(GitHubAppRuntimeConfig gitHubAppRuntimeConfig, HttpConfiguration httpConfiguration,
            ObjectMapper objectMapper) {
        if (!gitHubAppRuntimeConfig.webhookProxyUrl.isPresent()) {
            this.eventSource = null;
            return;
        }

        String localUrl = "http://" + httpConfiguration.host + ":" + httpConfiguration.port + "/";
        this.eventSource = startEventSource(gitHubAppRuntimeConfig.webhookProxyUrl.get(), localUrl, new OkHttpClient(),
                objectMapper);
    }

    void stopEventSource(@Observes ShutdownEvent shutdownEvent) {
        if (eventSource != null) {
            eventSource.close();
        }
    }

    private static EventSource startEventSource(String webhookProxyUrl, String localUrl, OkHttpClient client,
            ObjectMapper objectMapper) {
        EventSource.Builder builder = new EventSource.Builder(new SimpleEventHandler(localUrl, client, objectMapper),
                URI.create(webhookProxyUrl))
                .reconnectTime(Duration.ofMillis(3000));

        EventSource eventSource = builder.build();
        eventSource.start();

        return eventSource;
    }

    private static class SimpleEventHandler implements EventHandler {

        private final OkHttpClient client;

        private final String localUrl;

        private final ObjectMapper objectMapper;

        private SimpleEventHandler(String localUrl, OkHttpClient client, ObjectMapper objectMapper) {
            this.client = client;
            this.localUrl = localUrl;
            this.objectMapper = objectMapper;
        }

        @Override
        public void onOpen() throws Exception {
        }

        @Override
        public void onClosed() throws Exception {
        }

        @Override
        public void onMessage(String event, MessageEvent messageEvent) throws Exception {
            if (EMPTY_MESSAGE.equals(messageEvent.getData())) {
                return;
            }

            JsonNode rootNode = objectMapper.readTree(messageEvent.getData());
            JsonNode body = rootNode.get("body");

            if (body != null) {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(localUrl)
                        .post(RequestBody.create(JSON, objectMapper.writeValueAsString(rootNode.get("body"))));

                for (String forwardedHeader : FORWARDED_HEADERS) {
                    JsonNode headerValue = rootNode.get(forwardedHeader.toLowerCase(Locale.ROOT));
                    if (headerValue != null && headerValue.isTextual()) {
                        requestBuilder.addHeader(forwardedHeader, headerValue.textValue());
                    }
                }

                try (Response response = client.newCall(requestBuilder.build()).execute()) {
                }
            }
        }

        @Override
        public void onComment(String comment) throws Exception {
        }

        @Override
        public void onError(Throwable t) {
        }
    }
}
