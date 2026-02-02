package io.quarkiverse.githubapp.it.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class CodestartKotlinTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartKotlinTest = QuarkusCodestartTest.builder()
            .languages(Language.KOTLIN)
            .setupStandaloneExtensionTest("io.quarkiverse.githubapp:quarkus-github-app")
            .putData(CreateProjectKey.PROJECT_NAME, "My GitHub App")
            .putData(CreateProjectKey.PROJECT_DESCRIPTION, "My GitHub App description")
            .build();

    @Test
    void testContent() throws Throwable {
        codestartKotlinTest.checkGeneratedSource("org.acme.MyGitHubApp");
        codestartKotlinTest.assertThatGeneratedFile(Language.KOTLIN, "pom.xml").exists();
        codestartKotlinTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, "README.md");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartKotlinTest.buildAllProjects();
    }
}
