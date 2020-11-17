package io.quarkiverse.githubapp.runtime.signing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;

@ApplicationScoped
public class PayloadSignatureChecker {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private final Mac mac;

    PayloadSignatureChecker(GitHubAppRuntimeConfig gitHubAppRuntimeConfig) {
        if (!gitHubAppRuntimeConfig.webhookSecret.isPresent()) {
            mac = null;
            return;
        }

        try {
            final SecretKeySpec keySpec = new SecretKeySpec(gitHubAppRuntimeConfig.webhookSecret.get().getBytes(UTF_8),
                    HMAC_SHA1_ALGORITHM);
            mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to initialize the payload signature checker", e);
        }
    }

    public boolean matches(String payload, String signature) {
        if (mac == null) {
            throw new IllegalStateException("Payload signature checking is disabled");
        }

        final byte[] payloadHmacBytes = mac.doFinal(payload.getBytes(UTF_8));
        final byte[] signatureBytes = signature.getBytes(UTF_8);

        return MessageDigest.isEqual(payloadHmacBytes, signatureBytes);
    }
}