package io.quarkiverse.githubapp.runtime.smee;

import static io.quarkiverse.githubapp.runtime.Headers.FORWARDED_HEADERS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.runtime.sse.EventStreamListener;
import io.quarkiverse.githubapp.runtime.sse.HttpEventStreamClient;
import io.quarkiverse.githubapp.runtime.sse.HttpEventStreamClient.Event;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@ApplicationScoped
@Startup
public class SmeeIoForwarder {

    private static final Logger LOG = Logger.getLogger(SmeeIoForwarder.class);

    private static final String EMPTY_MESSAGE = "{}";

    private final HttpEventStreamClient eventStreamClient;
    private final ReplayEventStreamAdapter replayEventStreamAdapter;

    @Inject
    SmeeIoForwarder(CheckedConfigProvider checkedConfigProvider, HttpConfiguration httpConfiguration,
            ObjectMapper objectMapper) {
        if (!checkedConfigProvider.webhookProxyUrl().isPresent()) {
            this.replayEventStreamAdapter = null;
            this.eventStreamClient = null;
            return;
        }

        LOG.info("Listening to events coming from " + checkedConfigProvider.webhookProxyUrl().get());

        URI localUrl = URI.create(
                "http://" + httpConfiguration.host + ":" + httpConfiguration.port + checkedConfigProvider.webhookUrlPath());

        this.replayEventStreamAdapter = new ReplayEventStreamAdapter(checkedConfigProvider.webhookProxyUrl().get(), localUrl,
                objectMapper);
        this.eventStreamClient = new HttpEventStreamClient(checkedConfigProvider.webhookProxyUrl().get(),
                this.replayEventStreamAdapter);
        this.eventStreamClient.setRetryCooldown(3000);
        this.eventStreamClient.start();
    }

    void stopEventSource(@Observes ShutdownEvent shutdownEvent) {
        if (this.replayEventStreamAdapter != null) {
            this.replayEventStreamAdapter.stop();
        }
        if (this.eventStreamClient != null) {
            this.eventStreamClient.stop();
        }
    }

    private static class ReplayEventStreamAdapter implements EventStreamListener {

        private final String proxyUrl;
        private final URI localUrl;
        private final ObjectMapper objectMapper;
        private final HttpClient forwardingHttpClient;

        private volatile boolean stopped = false;

        private ReplayEventStreamAdapter(String proxyUrl, URI localUrl, ObjectMapper objectMapper) {
            this.proxyUrl = proxyUrl;
            this.localUrl = localUrl;
            this.objectMapper = objectMapper;
            this.forwardingHttpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
        }

        @Override
        public void onEvent(HttpEventStreamClient client, Event event) {
            if (stopped) {
                return;
            }

            if (EMPTY_MESSAGE.equals(event.getData())) {
                return;
            }

            int startOfJsonObject = event.getData().indexOf('{');
            if (startOfJsonObject == -1) {
                return;
            }

            // for some reason, the message coming from smee.io sometimes includes a 'id: 123' at the beginning of the message
            // let's be safe and drop anything before the start of the JSON object.
            String data = event.getData().substring(startOfJsonObject);

            try {
                JsonNode rootNode = objectMapper.readTree(data);
                JsonNode body = rootNode.get("body");

                if (body != null) {
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(localUrl)
                            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(rootNode.get("body"))));

                    for (String forwardedHeader : FORWARDED_HEADERS) {
                        JsonNode headerValue = rootNode.get(forwardedHeader.toLowerCase(Locale.ROOT));
                        if (headerValue != null && headerValue.isTextual()) {
                            requestBuilder.header(forwardedHeader, headerValue.textValue());
                        }
                    }

                    forwardingHttpClient.send(requestBuilder.build(), BodyHandlers.discarding());
                }
            } catch (IOException e) {
                if (e.getMessage().contains("GOAWAY received")) {
                    // ignore
                } else {
                    LOG.error("An error occurred while forwarding a payload to the local application running in dev mode", e);
                }
            } catch (Exception e) {
                LOG.error("An error occurred while forwarding a payload to the local application running in dev mode", e);
            }
        }

        @Override
        public void onReconnect(HttpEventStreamClient client, HttpResponse<Void> response, boolean hasReceivedEvents,
                long lastEventID) {
            if (stopped) {
                return;
            }

            LOG.info("Reconnected to " + proxyUrl);
        }

        @Override
        public void onError(HttpEventStreamClient client, Throwable throwable) {
            if (stopped) {
                return;
            }

            LOG.error("An error occurred with Smee.io proxying", throwable);
        }

        @Override
        public void onClose(HttpEventStreamClient client, HttpResponse<Void> response) {
        }

        public void stop() {
            this.stopped = true;
        }
    }
}
