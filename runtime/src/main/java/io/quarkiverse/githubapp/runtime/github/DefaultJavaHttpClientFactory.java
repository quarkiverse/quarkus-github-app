package io.quarkiverse.githubapp.runtime.github;

import java.net.http.HttpClient;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Reserve;

@Dependent
@Reserve
@Priority(0)
public class DefaultJavaHttpClientFactory extends AbstractJavaHttpClientFactory {

    @Override
    public HttpClient create() {
        return createDefaultClientBuilder().build();
    }
}
