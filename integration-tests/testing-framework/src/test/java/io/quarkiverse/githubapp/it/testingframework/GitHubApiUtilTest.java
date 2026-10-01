package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppMockito.mockPagedIterable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.mockito.quality.Strictness;

import io.quarkiverse.githubapp.GitHubApiUtil;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@GitHubAppTest
@TestProfile(GitHubApiUtilTest.ThrottlingEnabledProfile.class)
class GitHubApiUtilTest {

    static final long THROTTLE_MILLIS = 100;

    public static class ThrottlingEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.github-app.rate-limit.throttle.enabled", "true",
                    "quarkus.github-app.rate-limit.throttle.read", "0.1s");
        }
    }

    @Test
    void toStream() {
        GHIssueComment comment1 = mock(GHIssueComment.class);
        GHIssueComment comment2 = mock(GHIssueComment.class);
        GHIssueComment comment3 = mock(GHIssueComment.class);

        PagedIterable<GHIssueComment> iterable = mockPagedIterable(comment1, comment2, comment3);

        List<GHIssueComment> result = GitHubApiUtil.toStream(iterable).toList();

        assertThat(result).containsExactly(comment1, comment2, comment3);
    }

    @Test
    void toStream_empty() {
        PagedIterable<GHIssueComment> iterable = mockPagedIterable();

        assertThat(GitHubApiUtil.toStream(iterable)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void toStream_throttlesBetweenPages() {
        List<String> page1 = List.of("a", "b");
        List<String> page2 = List.of("c", "d");
        List<String> page3 = List.of("e");

        PagedIterator<String> iteratorMock = mock(PagedIterator.class,
                withSettings().stubOnly().strictness(Strictness.LENIENT));
        List<List<String>> pages = new ArrayList<>(List.of(page1, page2, page3));
        when(iteratorMock.hasNext()).thenAnswer(ignored -> !pages.isEmpty());
        when(iteratorMock.nextPage()).thenAnswer(ignored -> pages.remove(0));

        PagedIterable<String> iterableMock = mock(PagedIterable.class,
                withSettings().stubOnly().strictness(Strictness.LENIENT));
        when(iterableMock.iterator()).thenReturn(iteratorMock);

        long start = System.nanoTime();
        List<String> result = GitHubApiUtil.toStream(iterableMock).toList();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(result).containsExactly("a", "b", "c", "d", "e");
        // 3 pages = 2 inter-page throttle delays
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(2 * THROTTLE_MILLIS);
    }
}
