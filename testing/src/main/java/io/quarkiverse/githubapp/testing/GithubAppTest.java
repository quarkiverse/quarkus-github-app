package io.quarkiverse.githubapp.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

import org.mockito.Answers;

/**
 * @deprecated Use {@link GitHubAppTest} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@GitHubAppTest
@Stereotype
@Deprecated
public @interface GithubAppTest {

    Answers defaultAnswers() default Answers.RETURNS_DEFAULTS;
}
