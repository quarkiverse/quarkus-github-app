package io.quarkiverse.githubapp.command.airline.deployment;

import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.ARGUMENTS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND_GROUP_METADATA;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND_METADATA;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.GLOBAL_METADATA;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.OPTION;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.FieldInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;

public class HideAirlineInjectAnnotationsTransformer implements AnnotationsTransformer {

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
                !COMMAND_METADATA.equals(fieldInfo.type().name())) {
            return;
        }

        transformationContext.transform().remove(ai -> DotNames.INJECT.equals(ai.name())).done();
    }
}
