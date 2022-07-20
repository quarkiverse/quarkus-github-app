package io.quarkiverse.githubapp.command.airline.deployment;

import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.ARGUMENTS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND_GROUP_METADATA;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND_METADATA;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.GLOBAL_METADATA;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.OPTION;

import java.util.Set;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;

public class HideAirlineInjectAnnotationsTransformer implements AnnotationsTransformer {

    private final IndexView index;

    HideAirlineInjectAnnotationsTransformer(IndexView index) {
        this.index = index;
    }

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.FIELD == kind;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        FieldInfo fieldInfo = transformationContext.getTarget().asField();

        if (!fieldInfo.hasAnnotation(DotNames.INJECT)) {
            return;
        }

        if (!fieldInfo.hasAnnotation(ARGUMENTS) &&
                !fieldInfo.hasAnnotation(OPTION) &&
                !GLOBAL_METADATA.equals(fieldInfo.type().name()) &&
                !COMMAND_GROUP_METADATA.equals(fieldInfo.type().name()) &&
                !COMMAND_METADATA.equals(fieldInfo.type().name()) &&
                !isComposition(fieldInfo)) {
            return;
        }

        transformationContext.transform().remove(ai -> DotNames.INJECT.equals(ai.name())).done();
    }

    private boolean isComposition(FieldInfo fieldInfo) {
        Type fieldType = fieldInfo.type();

        if (fieldType.kind() != Type.Kind.CLASS) {
            return false;
        }

        ClassInfo fieldClass = index.getClassByName(fieldType.asClassType().name());

        if (fieldClass == null) {
            return false;
        }

        Set<DotName> fieldClassAnnotations = fieldClass.annotations().keySet();

        return fieldClassAnnotations.contains(ARGUMENTS) || fieldClassAnnotations.contains(OPTION);
    }
}
