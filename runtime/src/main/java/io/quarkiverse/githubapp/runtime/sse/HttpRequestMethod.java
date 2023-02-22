package io.quarkiverse.githubapp.runtime.sse;

/**
 * Enum representing HTTP request methods supported by {@link HttpEventStreamClient}
 *
 * @author LupCode.com (Luca Vogels)
 * @since 2020-12-22
 */
public enum HttpRequestMethod {
    GET(false),
    POST(true),
    PUT(true),
    DELETE(false);

    private boolean body;

    HttpRequestMethod(boolean body) {
        this.body = body;
    }

    /**
     * Returns if method type needs a request body
     *
     * @return True if request body is needed
     */
    public boolean needsBody() {
        return body;
    }
}
