package io.quarkiverse.githubapp.testing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;
import org.mockito.Answers;
import org.mockito.exceptions.verification.NoInteractionsWanted;

public class GitHubMockContextImplEventMocksTest {

    private final GitHubMockContextImpl context = new GitHubMockContextImpl(Answers.RETURNS_DEFAULTS);
    private GitHub client;

    @BeforeAll
    public static void assertionsSettings() {
        Assertions.setMaxStackTraceElementsDisplayed(30);
    }

    @BeforeEach
    public void init() {
        context.init();
        client = context.client(123);
    }

    @Test
    public void getter_forwardedToActual() throws IOException {
        GHEventPayload.Issue event = parseEvent("/issue-opened.json", GHEventPayload.Issue.class);
        assertThat(event.getIssue().getTitle()).isEqualTo("test");

        Object[] allMocks = context.ghObjects();
        assertThat(allMocks).hasSize(1);
        assertThatCode(() -> verifyNoMoreInteractions(allMocks))
                .doesNotThrowAnyException();
    }

    @Test
    public void nonGetter_forwardedToMock_expectedCall_defaultBehavior() throws IOException {
        GHEventPayload.Issue event = parseEvent("/issue-opened.json", GHEventPayload.Issue.class);

        event.getIssue().addLabels("foo");

        assertThatCode(() -> verify(context.issue(750705278)).addLabels("foo"))
                .doesNotThrowAnyException();

        Object[] allMocks = context.ghObjects();
        assertThat(allMocks).hasSize(1);
        assertThatCode(() -> verifyNoMoreInteractions(allMocks))
                .doesNotThrowAnyException();
    }

    @Test
    public void nonGetter_forwardedToMock_expectedCall_customBehavior() throws IOException {
        GHEventPayload.Issue event = parseEvent("/issue-opened.json", GHEventPayload.Issue.class);

        RuntimeException myException = new RuntimeException();
        doThrow(myException)
                .when(context.issue(750705278))
                .addLabels("foo");

        assertThatThrownBy(() -> event.getIssue().addLabels("foo"))
                .isSameAs(myException);

        verify(context.issue(750705278)).addLabels("foo");

        Object[] allMocks = context.ghObjects();
        assertThat(allMocks).hasSize(1);
        assertThatCode(() -> verifyNoMoreInteractions(allMocks))
                .doesNotThrowAnyException();
    }

    @Test
    public void nonGetter_forwardedToMock_unexpectedCall() throws IOException {
        GHEventPayload.Issue event = parseEvent("/issue-opened.json", GHEventPayload.Issue.class);

        event.getIssue().addLabels("foo");

        Object[] allMocks = context.ghObjects();
        assertThatThrownBy(() -> verifyNoMoreInteractions(allMocks))
                .isInstanceOf(NoInteractionsWanted.class)
                .hasMessageContainingAll("No interactions wanted here:",
                        "-> at io.quarkiverse.githubapp.testing.internal.GitHubMockContextImplEventMocksTest.lambda$nonGetter_forwardedToMock_unexpectedCall",
                        "But found this interaction on mock 'GHIssue#750705278':",
                        "-> at io.quarkiverse.githubapp.testing.internal.GitHubMockContextImplEventMocksTest.nonGetter_forwardedToMock_unexpectedCall");
    }

    @Test
    public void nonGetter_varArgs() throws IOException {
        GHEventPayload.Issue event = parseEvent("/issue-opened.json", GHEventPayload.Issue.class);

        event.getIssue().addLabels("foo", "bar");

        assertThatCode(() -> verify(context.issue(750705278)).addLabels("foo", "bar"))
                .doesNotThrowAnyException();

        Object[] allMocks = context.ghObjects();
        assertThat(allMocks).hasSize(1);
        assertThatCode(() -> verifyNoMoreInteractions(allMocks))
                .doesNotThrowAnyException();
    }

    private <T extends GHEventPayload> T parseEvent(String resourcePath, Class<T> type) throws IOException {
        try (InputStream is = getClass().getResource(resourcePath).openStream();
                Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return client.parseEventPayload(reader, type);
        }
    }
}
