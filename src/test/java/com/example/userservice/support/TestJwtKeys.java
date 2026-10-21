package com.example.userservice.support;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;

/** Ephemeral test keys only. Never reads or changes the production key files. */
public final class TestJwtKeys {
    private static final KeyPair PAIR = generate();
    private static final Path PUBLIC_KEY = writePublicKey();
    private static final JwtEncoder ENCODER = NimbusJwtEncoder.withKeyPair(
            (RSAPublicKey) PAIR.getPublic(), (RSAPrivateKey) PAIR.getPrivate()).build();

    private TestJwtKeys() {}
    public static String publicKeyLocation() { return PUBLIC_KEY.toUri().toString(); }
    public static String access(long userId, String role) {
        return token(userId, role, "access", "authentication-service", "java-advanced-api",
                Instant.now().plusSeconds(900));
    }
    public static String token(long userId, String role, String type, String issuer,
                               String audience, Instant expiry) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(Long.toString(userId)).id(UUID.randomUUID().toString())
                .issuedAt(Instant.now().minusSeconds(600)).notBefore(Instant.now().minusSeconds(600))
                .expiresAt(expiry).claim("userId", userId).claim("role", role)
                .claim("tokenType", type).build();
        return ENCODER.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims)).getTokenValue();
    }
    private static KeyPair generate() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) { throw new ExceptionInInitializerError(exception); }
    }
    private static Path writePublicKey() {
        try {
            Path path = Files.createTempFile("user-jwt-test-", ".pem");
            Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(PAIR.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n", StandardCharsets.US_ASCII);
            path.toFile().deleteOnExit();
            return path;
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
}
