package io.quarkiverse.githubapp;

import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.enterprise.inject.spi.CDI;

import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;

/**
 * Utilities for proactive throttling of GitHub API calls,
 * to avoid triggering <a href="https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api">GitHub's
 * secondary rate limits</a>.
 * <p>
 * Throttle delays are configurable via {@code quarkus.github-app.rate-limit.throttle.*} properties.
 */
public final class GitHubApiUtil {

    private GitHubApiUtil() {
    }

    /**
     * Converts a {@link PagedIterable} to a {@link Stream} with proactive read throttling
     * between page fetches to avoid triggering GitHub's secondary rate limits.
     *
     * @param iterable the GitHub API paginated result
     * @param <T> the element type
     * @return a stream with built-in throttling
     */
    public static <T> Stream<T> toStream(PagedIterable<T> iterable) {
        PagedIterator<T> pagedIterator = iterable.iterator();
        Iterator<T> throttlingIterator = new ThrottlingIterator<>(pagedIterator);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(throttlingIterator, 0), false);
    }

    /**
     * Sleeps for the configured read throttle delay.
     * <p>
     * Call between read operations to proactively avoid triggering
     * GitHub's secondary rate limits.
     *
     * @see #toStream(PagedIterable) for automatic throttling during pagination
     */
    public static void sleepForReadThrottling() {
        GitHubAppRuntimeConfig config = config();
        if (config != null && config.rateLimit().throttle().enabled()) {
            sleep(config.rateLimit().throttle().read().toMillis());
        }
    }

    /**
     * Sleeps for the configured write throttle delay.
     * <p>
     * Call between write/delete operations to proactively avoid triggering
     * GitHub's secondary rate limits.
     */
    public static void sleepForWriteThrottling() {
        GitHubAppRuntimeConfig config = config();
        if (config != null && config.rateLimit().throttle().enabled()) {
            sleep(config.rateLimit().throttle().write().toMillis());
        }
    }

    private static GitHubAppRuntimeConfig config() {
        try {
            return CDI.current().select(GitHubAppRuntimeConfig.class).get();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(
                    (InterruptedIOException) new InterruptedIOException().initCause(e));
        }
    }

    private static final class ThrottlingIterator<T> implements Iterator<T> {

        private final PagedIterator<T> delegate;
        private Iterator<T> currentPage = Collections.emptyIterator();
        private boolean firstPage = true;

        ThrottlingIterator(PagedIterator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            if (currentPage.hasNext()) {
                return true;
            }
            if (!firstPage) {
                sleepForReadThrottling();
            }
            firstPage = false;
            if (!delegate.hasNext()) {
                return false;
            }
            currentPage = delegate.nextPage().iterator();
            return currentPage.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return currentPage.next();
        }
    }
}
