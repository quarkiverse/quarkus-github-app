package io.quarkiverse.githubapp;

import java.net.http.HttpClient;

/**
 * Allows to customize how the Java HTTP client used to contact the GitHub REST API is created.
 */
public interface JavaHttpClientFactory {

    HttpClient create();
}
