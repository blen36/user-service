package com.example.userservice.support;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class TestSecurityProperties {

    public static final String REGISTRATION_KEY =
            "test-registration-key-0123456789abcdef";

    private static final String PUBLIC_KEY_URI = createPublicKey();

    private TestSecurityProperties() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("user.jwt.public-key", () -> PUBLIC_KEY_URI);
        registry.add("user.registration-key", () -> REGISTRATION_KEY);
    }

    private static String createPublicKey() {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            String encoded = Base64.getMimeEncoder(
                    64,
                    new byte[]{'\n'}
            ).encodeToString(
                    generator.generateKeyPair().getPublic().getEncoded()
            );

            Path file = Files.createTempFile("user-test-public-", ".pem");

            Files.writeString(
                    file,
                    "-----BEGIN PUBLIC KEY-----\n"
                            + encoded
                            + "\n-----END PUBLIC KEY-----\n"
            );

            file.toFile().deleteOnExit();
            return file.toUri().toString();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot create test public key",
                    exception
            );
        }
    }
}