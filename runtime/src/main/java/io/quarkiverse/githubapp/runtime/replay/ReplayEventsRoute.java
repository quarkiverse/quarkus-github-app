package io.quarkiverse.githubapp.runtime.replay;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Singleton;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.ext.web.RoutingContext;

@Singleton
@IfBuildProfile("dev")
public class ReplayEventsRoute {

    /**
     * The past events are recorded here so that, when you connect/reconnect, you get the past events.
     */
    private final ConcurrentLinkedQueue<ReplayEvent> recordedEvents = new ConcurrentLinkedQueue<>();

    private final BroadcastProcessor<ReplayEvent> broadcastProcessor = BroadcastProcessor.create();

    @Route(path = "/replay/events", produces = ReactiveRoutes.EVENT_STREAM)
    Multi<ReplayEvent> replayEvents(RoutingContext context) {
        return Multi.createBy()
                .merging().streams(
                        Multi.createFrom().iterable(recordedEvents),
                        broadcastProcessor.onOverflow().drop(),
                        Multi.createFrom().ticks().every(Duration.ofMillis(100)).onOverflow()
                                .drop().map(x -> ReplayEvent.PING));
    }

    public void pushEvent(GitHubEvent gitHubEvent) {
        ReplayEvent replayEvent = new ReplayEvent(gitHubEvent);

        recordedEvents.add(replayEvent);
        broadcastProcessor.onNext(replayEvent);
    }
}
