package io.quarkiverse.githubapp.testing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Iterator;
import java.util.List;

import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Answers;
import org.mockito.quality.Strictness;

public final class GitHubAppMockito {

    private GitHubAppMockito() {
    }

    public static <T> T mockBuilder(Class<T> builderClass) {
        return mock(builderClass, withSettings().defaultAnswer(Answers.RETURNS_SELF));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> PagedSearchIterable<T> mockPagedIterable(T... contentMocks) {
        PagedSearchIterable<T> iterableMock = mock(PagedSearchIterable.class,
                withSettings().stubOnly().strictness(Strictness.LENIENT).defaultAnswer(Answers.RETURNS_SELF));
        when(iterableMock.spliterator()).thenAnswer(ignored -> List.of(contentMocks).spliterator());
        when(iterableMock.iterator()).thenAnswer(ignored -> {
            PagedIterator<T> iteratorMock = mock(PagedIterator.class, withSettings().stubOnly().strictness(Strictness.LENIENT));
            Iterator<T> actualIterator = List.of(contentMocks).iterator();
            when(iteratorMock.next()).thenAnswer(ignored2 -> actualIterator.next());
            when(iteratorMock.hasNext()).thenAnswer(ignored2 -> actualIterator.hasNext());
            return iteratorMock;
        });
        return iterableMock;
    }

}
