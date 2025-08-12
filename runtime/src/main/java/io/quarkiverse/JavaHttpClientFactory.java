package io.quarkiverse;

import java.net.http.HttpClient;

public interface JavaHttpClientFactory {

    HttpClient create();
}
