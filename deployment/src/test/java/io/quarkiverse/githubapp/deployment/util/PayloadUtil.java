package io.quarkiverse.githubapp.deployment.util;

import java.nio.charset.StandardCharsets;

public class PayloadUtil {

    public static String getPayload(String resourceName) {
        try {
            return new String(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName).readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read payload: " + resourceName, e);
        }
    }

    private PayloadUtil() {
    }
}
