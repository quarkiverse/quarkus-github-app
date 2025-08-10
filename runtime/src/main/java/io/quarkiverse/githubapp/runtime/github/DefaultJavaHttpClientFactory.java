package io.quarkiverse.githubapp.runtime.github;

import java.net.http.HttpClient;

import jakarta.enterprise.context.Dependent;

import io.quarkus.arc.DefaultBean;

@Dependent
@DefaultBean
public class DefaultJavaHttpClientFactory extends AbstractJavaHttpClientFactory {

    @Override
    public HttpClient create() {
        return createDefaultClientBuilder().build();
    }
}
