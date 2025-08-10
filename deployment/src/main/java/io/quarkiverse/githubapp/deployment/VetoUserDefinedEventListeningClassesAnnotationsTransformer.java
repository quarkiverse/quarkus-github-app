package io.quarkiverse.githubapp.deployment;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.DotNames;

class VetoUserDefinedEventListeningClassesAnnotationsTransformer implements AnnotationTransformation {

    private final Set<DotName> eventDefinitionAnnotations;

    VetoUserDefinedEventListeningClassesAnnotationsTransformer(Set<DotName> eventDefinitionAnnotations) {
        this.eventDefinitionAnnotations = eventDefinitionAnnotations;
    }

    @Override
    public boolean supports(AnnotationTarget.Kind kind) {
        return Kind.CLASS.equals(kind);
    }

    @Override
    public void apply(TransformationContext transformationContext) {
        if (transformationContext.hasAnnotation(GitHubAppDotNames.MULTIPLEXER)) {
            return;
        }

        if (isEventListeningClass(transformationContext)) {
            transformationContext.add(AnnotationInstance.builder(DotNames.VETOED).build());
        }
    }

    public boolean isEventListeningClass(TransformationContext transformationContext) {
        if (transformationContext.hasAnnotation(GitHubAppDotNames.RAW_EVENT)) {
            return true;
        }

        for (DotName eventDefiningAnnotation : eventDefinitionAnnotations) {
            if (!transformationContext.hasAnnotation(eventDefiningAnnotation)) {
                continue;
            }

            return true;
        }

        return false;
    }
}
