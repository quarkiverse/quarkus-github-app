package io.quarkiverse.githubapp.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Singleton
public class UtilsProducer {

    @Produces
    @Singleton
    @Yaml
    public YAMLMapper yamlObjectMapper() {
        return YAMLMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Target({ METHOD, FIELD, PARAMETER, TYPE })
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Yaml {
    }
}
