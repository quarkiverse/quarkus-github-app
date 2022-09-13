package io.quarkiverse.githubapp.runtime.signing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Singleton;

import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;

@Singleton
public class PayloadSignatureChecker {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String HEADER_SIGNATURE_PREFIX = "sha256=";

    private final SecretKeySpec secretKeySpec;
    private final Mac sharedMac;
    private final boolean supportsClone;

    PayloadSignatureChecker(CheckedConfigProvider checkedConfigProvider) {
        if (!checkedConfigProvider.webhookSecret().isPresent()) {
            secretKeySpec = null;
            sharedMac = null;
            supportsClone = false;
            return;
        }

        secretKeySpec = new SecretKeySpec(checkedConfigProvider.webhookSecret().get().getBytes(UTF_8),
                HMAC_SHA256_ALGORITHM);
        sharedMac = createNewMacInstance(secretKeySpec);
        supportsClone = supportsClone(sharedMac);
    }

    public boolean matches(byte[] payload, String headerSignature) {
        if (secretKeySpec == null || sharedMac == null) {
            throw new IllegalStateException("Payload signature checking is disabled, this method should not be called");
        }

        String payloadSignature = hex(getMacInstance().doFinal(payload));

        // we need a constant time equals thus why we don't use String.equals()
        return MessageDigest.isEqual(payloadSignature.getBytes(),
                headerSignature.substring(HEADER_SIGNATURE_PREFIX.length()).getBytes());
    }

    public static boolean supportsClone(Mac mac) {
        try {
            mac.clone();
            return true;
        } catch (CloneNotSupportedException e) {
            return false;
        }
    }

    public Mac getMacInstance() {
        if (supportsClone) {
            try {
                return (Mac) sharedMac.clone();
            } catch (CloneNotSupportedException e) {
                // should never happen but let's fallback anyway
            }
        }
        return createNewMacInstance(secretKeySpec);
    }

    public static Mac createNewMacInstance(SecretKeySpec secretKeySpec) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to initialize the payload signature checker", e);
        }
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }
}