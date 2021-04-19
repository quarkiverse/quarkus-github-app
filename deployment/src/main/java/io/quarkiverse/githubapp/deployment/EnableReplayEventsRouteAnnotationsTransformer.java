package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.AnnotationTarget.Kind;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;

public final class EnableReplayEventsRouteAnnotationsTransformer implements AnnotationsTransformer {

    static final EnableReplayEventsRouteAnnotationsTransformer INSTANCE = new EnableReplayEventsRouteAnnotationsTransformer();

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.CLASS.equals(kind);
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        if (!GitHubAppDotNames.REPLAY_EVENTS_ROUTE.equals(transformationContext.getTarget().asClass().name())) {
            return;
        }
        transformationContext.transform().remove(ai -> DotNames.VETOED.equals(ai.name())).done();
    }

    private EnableReplayEventsRouteAnnotationsTransformer() {
    }
}
