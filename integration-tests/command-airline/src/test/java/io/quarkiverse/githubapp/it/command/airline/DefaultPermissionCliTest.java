package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.ReactionContent;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class DefaultPermissionCliTest {

    @Test
    void testNoPermissionSet() throws IOException {
        given()
                .github(mocks -> {
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.READ)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.WRITE)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.ADMIN)))
                            .thenReturn(false);
                })
                .when().payloadFromClasspath("/issue-comment-default-permission-no-permission.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyHasPermission(mocks);
                    verifyCommandExecution(mocks, "hello from @default-permission test-no-permission");
                });

        given()
                .github(mocks -> {
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.READ)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.WRITE)))
                            .thenReturn(false);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.ADMIN)))
                            .thenReturn(false);
                })
                .when().payloadFromClasspath("/issue-comment-default-permission-no-permission.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyHasPermission(mocks);
                    verifyPermissionDenied(mocks);
                });
    }

    @Test
    void testPermissionOverriddenRead() throws IOException {
        given()
                .github(mocks -> {
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.READ)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.WRITE)))
                            .thenReturn(false);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.ADMIN)))
                            .thenReturn(false);
                })
                .when().payloadFromClasspath("/issue-comment-default-permission-read-permission.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyHasPermission(mocks);
                    verifyCommandExecution(mocks, "hello from @default-permission test-read-permission");
                });

        given()
                .github(mocks -> {
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.READ)))
                            .thenReturn(false);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.WRITE)))
                            .thenReturn(false);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.ADMIN)))
                            .thenReturn(false);
                })
                .when().payloadFromClasspath("/issue-comment-default-permission-read-permission.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyHasPermission(mocks);
                    verifyPermissionDenied(mocks);
                });
    }

    @Test
    void testPermissionOverriddenAdmin() throws IOException {
        given()
                .github(mocks -> {
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.READ)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.WRITE)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.ADMIN)))
                            .thenReturn(true);
                })
                .when().payloadFromClasspath("/issue-comment-default-permission-admin-permission.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyHasPermission(mocks);
                    verifyCommandExecution(mocks, "hello from @default-permission test-admin-permission");
                });

        given()
                .github(mocks -> {
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.READ)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.WRITE)))
                            .thenReturn(true);
                    Mockito.when(mocks.repository("quarkus-bot-java-playground").hasPermission(any(GHUser.class),
                            eq(GHPermissionType.ADMIN)))
                            .thenReturn(false);
                })
                .when().payloadFromClasspath("/issue-comment-default-permission-admin-permission.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyHasPermission(mocks);
                    verifyPermissionDenied(mocks);
                });
    }

    private void verifyHasPermission(GitHubMockVerificationContext mocks) throws IOException {
        verify(mocks.repository("quarkus-bot-java-playground")).hasPermission(any(GHUser.class),
                any(GHPermissionType.class));
    }

    private void verifyPermissionDenied(GitHubMockVerificationContext mocks) throws IOException {
        verify(mocks.issueComment(1093016219))
                .createReaction(ReactionContent.MINUS_ONE);
        verifyNoMoreInteractions(mocks.ghObjects());
    }
}
