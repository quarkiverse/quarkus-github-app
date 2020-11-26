package io.quarkiverse.githubapp.deployment.util;

import java.nio.charset.StandardCharsets;

public class PayloadUtil {

    public static String getPayload(String resourceName) {
        return new String(getPayloadAsBytes(resourceName), StandardCharsets.UTF_8);
    }

    public static byte[] getPayloadAsBytes(String resourceName) {
        try {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName).readAllBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read payload: " + resourceName, e);
        }
    }

    private PayloadUtil() {
    }
}
