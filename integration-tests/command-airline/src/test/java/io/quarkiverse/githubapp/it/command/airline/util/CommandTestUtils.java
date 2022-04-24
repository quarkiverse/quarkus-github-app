package io.quarkiverse.githubapp.it.command.airline.util;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;

public final class CommandTestUtils {

    private CommandTestUtils() {
    }

    public static void verifyCommandExecution(GitHubMockVerificationContext mocks, String comment) throws IOException {
        verify(mocks.issueComment(1093016219))
                .createReaction(ReactionContent.ROCKET);
        verify(mocks.issue(1168785554))
                .comment(comment);
        verify(mocks.issueComment(1093016219))
                .createReaction(ReactionContent.PLUS_ONE);
        verifyNoMoreInteractions(mocks.ghObjects());
    }
}
