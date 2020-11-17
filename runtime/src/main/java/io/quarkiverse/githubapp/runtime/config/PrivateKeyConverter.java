package io.quarkiverse.githubapp.runtime.config;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkiverse.githubapp.runtime.signing.PrivateKeyUtil;
import io.quarkus.runtime.configuration.ConfigurationException;

@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class PrivateKeyConverter implements Converter<PrivateKey>, Serializable {

    public PrivateKeyConverter() {
    }

    @Override
    public PrivateKey convert(final String value) {
        String privateKeyValue = value.trim();

        if (privateKeyValue.isEmpty()) {
            return null;
        }

        try {
            return PrivateKeyUtil.loadKey(privateKeyValue);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException("Unable to interpret the provided private key", e);
        }
    }
}
