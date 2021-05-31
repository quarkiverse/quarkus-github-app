package io.quarkiverse.githubapp.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

import org.mockito.Answers;

import io.quarkus.test.common.QuarkusTestResource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@QuarkusTestResource(GitHubAppTestingResource.class)
@Stereotype
public @interface GithubAppTest {

    Answers defaultAnswers() default Answers.RETURNS_DEFAULTS;
}
