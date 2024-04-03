package io.quarkiverse.githubapp.deployment;

import java.util.Map;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubapp.Credentials;
import io.quarkiverse.githubapp.event.Label;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class CredentialsProviderTest {

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ListeningClass.class, CustomCredentialsProvider.class)
                    .addAsResource("application-for-prod.properties", "application.properties"))
            .setRun(true);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testConfigFileOrder() {
        // the fact that the application started is actually enough to validate the CredentialsProvider is active
    }

    static class ListeningClass {

        void createLabel(@Label.Created GHEventPayload.Label labelPayload) {
        }
    }

    @Singleton
    public static class CustomCredentialsProvider implements CredentialsProvider {

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            if (!"my-github-app".equals(credentialsProviderName)) {
                throw new IllegalStateException("We expect my-github-app as the provider name");
            }

            return Map.of(Credentials.WEBHOOK_SECRET, "my webhook secret",
                    Credentials.PRIVATE_KEY, "-----BEGIN RSA PRIVATE KEY-----\n"
                            + "MIICWwIBAAKBgQCBbwkBgQDHP3iTKDuneQekMrkNfjZyGCl9pdvsoX1MwPAUW+tq\n"
                            + "mcN6N+cNvUTQz91YKjB/m4sYyuP30ZhegMwXI/AbHIncG6pDGnEmz9snRyaeqyyD\n"
                            + "1x81XX5WBepqOYN9fVH81IrhLIngCzpruhLgL8b+ZBUY7DnUANzQpWrd/QIDAQAB\n"
                            + "AoGAdV3c+bsjnIkmabIq3cK2tiK0gNK4xh64yNG0Kc+J0iaFzMBJKYHCqrm0T1YX\n"
                            + "540FdiPTlHLT36hirV4mX1NFPEG0qmNkGRaNCAEI7PQ8TEslAPvGRL79p7RO0oE8\n"
                            + "DE2P+ePC6InHYevQYfk0vi27ZoL2+7tOnaDZxVXTjfR/rrUCQQD0+nZveM8jBUMh\n"
                            + "Apw6rhedIYFrhTiH1YlDKlYqeiViWb9AhZdI12k7XGRDIPpQnZ6zIO/VtRMMNgoX\n"
                            + "9RsWdL/zAkEAh0HJowOvj/OPGLuNUmDKlijivssCCYD/rcKxkjh/a3H2J2k5sY7y\n"
                            + "v4maQb+t+keYqHlhxM+z/pyGhkYop3hWTwJAYdLqHFVHkZp2VeYu8Je4Qjyw63iF\n"
                            + "PGieqT1srwWbjAx+fItb//BUyyl3t/6hNjPavXj3jIUEGCo0GaD8shjo1QJAWkra\n"
                            + "to4xVyG6t0INF58x3ogwxjlzhLCu/mpobDp3JV0QfELMlvHcr2zGo3m4RMoi6OUP\n"
                            + "FXmqqSAI1f5kCVhWFQJAKSSwfkx3jOKtrkmJygi69NdwWOIe+Q9cI52KtzSWhZWq\n"
                            + "0Ih2KR36ODkbB6AYts+U2L5hiY9kTdffEFzH4qj8qw==\n"
                            + "-----END RSA PRIVATE KEY-----");
        }
    }
}
