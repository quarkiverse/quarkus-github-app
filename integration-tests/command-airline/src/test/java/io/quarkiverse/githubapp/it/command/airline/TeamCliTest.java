package io.quarkiverse.githubapp.it.command.airline;

import static io.quarkiverse.githubapp.it.command.airline.util.CommandTestUtils.verifyCommandExecution;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.ReactionContent;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GitHubAppTest
public class TeamCliTest {

    @Test
    void testTeam1() throws IOException {
        given()
                .github(mocks -> {
                    Set<GHTeam> teams = new LinkedHashSet<>();

                    GHTeam team1 = mocks.team(1L);
                    Mockito.when(team1.getSlug()).thenReturn("my-team-1");
                    Mockito.when(team1.hasMember(any(GHUser.class))).thenReturn(true);
                    teams.add(team1);

                    GHTeam team2 = mocks.team(2L);
                    Mockito.when(team2.getSlug()).thenReturn("my-team-2");
                    Mockito.when(team2.hasMember(any(GHUser.class))).thenReturn(false);
                    teams.add(team2);

                    Mockito.when(mocks.repository("gsmet/quarkus-bot-java-playground").getTeams())
                            .thenReturn(teams);
                })
                .when().payloadFromClasspath("/issue-comment-team-team1.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyGetTeams(mocks);
                    verifyGetSlug(mocks, 1L);
                    verifyGetSlug(mocks, 2L);
                    verifyHasMember(mocks, 1L);
                    verifyCommandExecution(mocks, "hello from @team team1");
                });

        given()
                .github(mocks -> {
                    Set<GHTeam> teams = new HashSet<>();

                    GHTeam team1 = mocks.team(1L);
                    Mockito.when(team1.getSlug()).thenReturn("my-team-1");
                    Mockito.when(team1.hasMember(any(GHUser.class))).thenReturn(false);
                    teams.add(team1);

                    GHTeam team2 = mocks.team(2L);
                    Mockito.when(team2.getSlug()).thenReturn("my-team-2");
                    Mockito.when(team2.hasMember(any(GHUser.class))).thenReturn(true);
                    teams.add(team2);

                    Mockito.when(mocks.repository("gsmet/quarkus-bot-java-playground").getTeams())
                            .thenReturn(teams);
                })
                .when().payloadFromClasspath("/issue-comment-team-team1.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyGetTeams(mocks);
                    verifyGetSlug(mocks, 1L);
                    verifyGetSlug(mocks, 2L);
                    verifyHasMember(mocks, 1L);
                    verifyPermissionDenied(mocks);
                });
    }

    @Test
    void testTwoTeams() throws IOException {
        given()
                .github(mocks -> {
                    Set<GHTeam> teams = new LinkedHashSet<>();

                    GHTeam team1 = mocks.team(1L);
                    Mockito.when(team1.getSlug()).thenReturn("my-team-1");
                    Mockito.when(team1.hasMember(any(GHUser.class))).thenReturn(true);
                    teams.add(team1);

                    GHTeam team2 = mocks.team(2L);
                    Mockito.when(team2.getSlug()).thenReturn("my-team-2");
                    Mockito.when(team2.hasMember(any(GHUser.class))).thenReturn(false);
                    teams.add(team2);

                    Mockito.when(mocks.repository("gsmet/quarkus-bot-java-playground").getTeams())
                            .thenReturn(teams);
                })
                .when().payloadFromClasspath("/issue-comment-team-two-teams.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyGetTeams(mocks);
                    verifyGetSlug(mocks, 1L);
                    verifyGetSlug(mocks, 2L);
                    verifyHasMember(mocks, 1L);
                    verifyCommandExecution(mocks, "hello from @team two-teams");
                });

        given()
                .github(mocks -> {
                    Set<GHTeam> teams = new LinkedHashSet<>();

                    GHTeam team1 = mocks.team(1L);
                    Mockito.when(team1.getSlug()).thenReturn("my-team-1");
                    Mockito.when(team1.hasMember(any(GHUser.class))).thenReturn(false);
                    teams.add(team1);

                    GHTeam team2 = mocks.team(2L);
                    Mockito.when(team2.getSlug()).thenReturn("my-team-2");
                    Mockito.when(team2.hasMember(any(GHUser.class))).thenReturn(true);
                    teams.add(team2);

                    Mockito.when(mocks.repository("gsmet/quarkus-bot-java-playground").getTeams())
                            .thenReturn(teams);
                })
                .when().payloadFromClasspath("/issue-comment-team-two-teams.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyGetTeams(mocks);
                    verifyGetSlug(mocks, 1L);
                    verifyGetSlug(mocks, 2L);
                    verifyHasMember(mocks, 1L);
                    verifyHasMember(mocks, 2L);
                    verifyCommandExecution(mocks, "hello from @team two-teams");
                });

        given()
                .github(mocks -> {
                    Set<GHTeam> teams = new HashSet<>();

                    GHTeam team1 = mocks.team(1L);
                    Mockito.when(team1.getSlug()).thenReturn("my-team-1");
                    Mockito.when(team1.hasMember(any(GHUser.class))).thenReturn(false);
                    teams.add(team1);

                    GHTeam team2 = mocks.team(2L);
                    Mockito.when(team2.getSlug()).thenReturn("my-team-2");
                    Mockito.when(team2.hasMember(any(GHUser.class))).thenReturn(false);
                    teams.add(team2);

                    GHTeam team3 = mocks.team(3L);
                    Mockito.when(team3.getSlug()).thenReturn("my-team-3");
                    Mockito.when(team3.hasMember(any(GHUser.class))).thenReturn(true);
                    teams.add(team3);

                    Mockito.when(mocks.repository("gsmet/quarkus-bot-java-playground").getTeams())
                            .thenReturn(teams);
                })
                .when().payloadFromClasspath("/issue-comment-team-two-teams.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
                    verifyGetTeams(mocks);
                    verifyGetSlug(mocks, 1L);
                    verifyGetSlug(mocks, 2L);
                    verifyGetSlug(mocks, 3L);
                    verifyHasMember(mocks, 1L);
                    verifyHasMember(mocks, 2L);
                    verifyPermissionDenied(mocks);
                });
    }

    private void verifyGetTeams(GitHubMockVerificationContext mocks) throws IOException {
        verify(mocks.repository("gsmet/quarkus-bot-java-playground")).getTeams();
    }

    private void verifyGetSlug(GitHubMockVerificationContext mocks, long teamId) throws IOException {
        verify(mocks.team(teamId)).getSlug();
    }

    private void verifyHasMember(GitHubMockVerificationContext mocks, long teamId) throws IOException {
        verify(mocks.team(teamId)).hasMember(any());
    }

    private void verifyPermissionDenied(GitHubMockVerificationContext mocks) throws IOException {
        verify(mocks.issueComment(1093016219))
                .createReaction(ReactionContent.MINUS_ONE);
        verifyNoMoreInteractions(mocks.ghObjects());
    }
}
