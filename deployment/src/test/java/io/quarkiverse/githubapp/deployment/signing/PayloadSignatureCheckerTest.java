package io.quarkiverse.githubapp.deployment.signing;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.githubapp.deployment.util.PayloadUtil;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkus.test.QuarkusUnitTest;

public class PayloadSignatureCheckerTest {

    private static final String PAYLOAD = "payloads/payload-signature-checker.json";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(PayloadUtil.class)
                    .addAsResource(PAYLOAD))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.github-app.webhook-secret", "cs5WKyOlY6go0E1RbkAOf0jq5K4KWBcP");

    @Inject
    PayloadSignatureChecker payloadSignatureChecker;

    @Test
    public void testPayloadSignatureCheck() {
        assertThat(payloadSignatureChecker.matches(
                PayloadUtil.getPayloadAsBytes(PAYLOAD),
                "sha256=360cb83c969705ab4d8a3b2b78ce9f875fb48096765aaeb71258dd4ba891fa2b")).isTrue();
    }
}
