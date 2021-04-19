package io.quarkiverse.githubapp.runtime.replay;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ReplayEventsRoute {

    @Inject
    LaunchMode launchMode;

    /**
     * The past events are recorded here so that, when you connect/reconnect, you get the past events.
     */
    private final ConcurrentLinkedQueue<ReplayEvent> recordedEvents = new ConcurrentLinkedQueue<>();

    private final BroadcastProcessor<ReplayEvent> broadcastProcessor = BroadcastProcessor.create();

    @Route(path = "/replay/events", produces = "text/event-stream")
    Multi<ReplayEvent> replayEvents(RoutingContext context) {
        if (launchMode != LaunchMode.DEVELOPMENT) {
            // TODO: we cannot set a 404 for now as MultiSupport doesn't support it
            // context.fail(404);
            return Multi.createFrom().empty();
        }

        return ReactiveRoutes.asEventStream(Multi.createBy()
                .merging().streams(
                        Multi.createFrom().iterable(recordedEvents),
                        broadcastProcessor.onOverflow().drop(),
                        Multi.createFrom().ticks().every(Duration.ofMillis(100)).onOverflow()
                                .drop().map(x -> ReplayEvent.PING)));
    }

    public void pushEvent(GitHubEvent gitHubEvent) {
        ReplayEvent replayEvent = new ReplayEvent(gitHubEvent);

        recordedEvents.add(replayEvent);
        broadcastProcessor.onNext(replayEvent);
    }
}
