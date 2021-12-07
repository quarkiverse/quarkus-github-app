package io.quarkiverse.githubapp.runtime.signing;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

import javax.inject.Singleton;

import io.smallrye.jwt.build.Jwt;

@Singleton
public class JwtTokenCreator {

    public String createJwtToken(String githubAppId, PrivateKey privateKey, long ttlSeconds)
            throws GeneralSecurityException, IOException {
        // Let's set the JWT Claims
        var jwtClaimsBuilder = Jwt.issuer(githubAppId);

        // If it has been specified, let's add the expiration
        if (ttlSeconds > 0) {
            jwtClaimsBuilder.expiresIn(ttlSeconds);
        }

        // Builds the JWT and serializes it to a compact, URL-safe string
        return jwtClaimsBuilder.sign(privateKey);
    }
}
