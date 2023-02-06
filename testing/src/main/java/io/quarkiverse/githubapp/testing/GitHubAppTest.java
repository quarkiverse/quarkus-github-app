package io.quarkiverse.githubapp.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Stereotype;

import org.mockito.Answers;

import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingResource;
import io.quarkus.test.common.QuarkusTestResource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@QuarkusTestResource(GitHubAppTestingResource.class)
@Stereotype
public @interface GitHubAppTest {

    Answers defaultAnswers() default Answers.RETURNS_DEFAULTS;
}
