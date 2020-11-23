package io.quarkiverse.githubapp.runtime.signing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Singleton;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;

@Singleton
public class PayloadSignatureChecker {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String HEADER_SIGNATURE_PREFIX = "sha256=";

    private final Mac mac;

    PayloadSignatureChecker(GitHubAppRuntimeConfig gitHubAppRuntimeConfig) {
        if (!gitHubAppRuntimeConfig.webhookSecret.isPresent()) {
            mac = null;
            return;
        }

        try {
            final SecretKeySpec keySpec = new SecretKeySpec(gitHubAppRuntimeConfig.webhookSecret.get().getBytes(UTF_8),
                    HMAC_SHA256_ALGORITHM);
            mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to initialize the payload signature checker", e);
        }
    }

    public boolean matches(String payload, String headerSignature) {
        if (mac == null) {
            throw new IllegalStateException("Payload signature checking is disabled");
        }

        String payloadSignature = hex(mac.doFinal(payload.getBytes(UTF_8)));

        // we need a constant time equals thus why we don't use String.equals()
        return MessageDigest.isEqual(payloadSignature.getBytes(),
                headerSignature.substring(HEADER_SIGNATURE_PREFIX.length()).getBytes());
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }
}