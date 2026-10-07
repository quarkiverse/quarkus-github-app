package io.quarkiverse.githubapp.deployment.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;

public class ReplayEventRouteAvailableInDevModeTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-for-dev.properties", "application.properties"));

    @Test
    public void testReplayEventsRouteAvailable() throws Exception {
        CompletableFuture<Integer> statusFuture = new CompletableFuture<>();
        CompletableFuture<String> bodyFuture = new CompletableFuture<>();

        Vertx vertx = Vertx.vertx();
        try {
            HttpClient client = vertx.createHttpClient();
            client.request(HttpMethod.GET, RestAssured.port, "localhost", "/replay/events")
                    .compose(HttpClientRequest::send)
                    .onSuccess(response -> {
                        statusFuture.complete(response.statusCode());
                        response.handler(buffer -> {
                            if (!bodyFuture.isDone()) {
                                bodyFuture.complete(buffer.toString());
                            }
                        });
                    })
                    .onFailure(statusFuture::completeExceptionally);

            assertEquals(200, statusFuture.get(10, TimeUnit.SECONDS));
            String firstChunk = bodyFuture.get(10, TimeUnit.SECONDS);
            assertFalse(firstChunk.isEmpty());
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }
}
