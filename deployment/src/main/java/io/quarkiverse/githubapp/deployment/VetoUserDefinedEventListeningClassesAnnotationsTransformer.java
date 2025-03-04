package io.quarkiverse.githubapp.deployment;

import java.util.Set;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;

class VetoUserDefinedEventListeningClassesAnnotationsTransformer implements AnnotationsTransformer {

    private final Set<DotName> eventDefinitionAnnotations;

    VetoUserDefinedEventListeningClassesAnnotationsTransformer(Set<DotName> eventDefinitionAnnotations) {
        this.eventDefinitionAnnotations = eventDefinitionAnnotations;
    }

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.CLASS.equals(kind);
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        Set<DotName> annotations = transformationContext.getTarget().asClass().annotationsMap().keySet();

        if (annotations.contains(GitHubAppDotNames.MULTIPLEXER)) {
            return;
        }

        if (isEventListeningClass(annotations)) {
            transformationContext.transform().add(DotNames.VETOED).done();
        }
    }

    public boolean isEventListeningClass(Set<DotName> annotations) {
        if (annotations.contains(GitHubAppDotNames.RAW_EVENT)) {
            return true;
        }

        for (DotName eventDefiningAnnotation : eventDefinitionAnnotations) {
            if (!annotations.contains(eventDefiningAnnotation)) {
                continue;
            }

            return true;
        }

        return false;
    }

}
