package io.quarkiverse.githubapp.it.command.airline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class CodestartTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .languages(QuarkusCodestartCatalog.Language.JAVA)
            .setupStandaloneExtensionTest("io.quarkiverse.githubapp:quarkus-github-app-command-airline")
            .putData(CreateProjectKey.PROJECT_NAME, "My GitHub Bot")
            .putData(CreateProjectKey.PROJECT_DESCRIPTION, "My GitHub Bot description")
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.MyGitHubBot");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "pom.xml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "README.md");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}