package com.innowise.userservice.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Ephemeral test keys only. Never reads or changes the production key files. */
public final class TestJwtKeys {
    private static final KeyPair PAIR = generate();
    private static final String JWKS_URI = serveJwks();
    private static final RSAKey SIGNING_KEY = new RSAKey.Builder((RSAPublicKey) PAIR.getPublic())
            .privateKey((RSAPrivateKey) PAIR.getPrivate()).keyID("test-key").build();
    private static final JwtEncoder ENCODER = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(SIGNING_KEY)));

    private TestJwtKeys() {}
    public static String jwksUri() { return JWKS_URI; }
    public static String access(long userId, String role) {
        return token(userId, role, "access", "authentication-service", "java-advanced-api",
                Instant.now().plusSeconds(900));
    }
    public static String token(long userId, String role, String type, String issuer,
                               String audience, Instant expiry) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(Long.toString(userId)).id(UUID.randomUUID().toString())
                .issuedAt(Instant.now().minusSeconds(600)).notBefore(Instant.now().minusSeconds(600))
                .expiresAt(expiry).claim("user_id", userId).claim("role", role)
                .claim("tokenType", type).build();
        return ENCODER.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").type("JWT").build(), claims)).getTokenValue();
    }
    private static KeyPair generate() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) { throw new ExceptionInInitializerError(exception); }
    }
    private static String serveJwks() {
        try {
            var publicKey = new RSAKey.Builder((RSAPublicKey) PAIR.getPublic()).keyID("test-key").build();
            byte[] body = new JWKSet(publicKey).toString().getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            String path = "/.well-known/jwks.json";
            server.createContext(path, exchange -> {
                try (exchange) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            });
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
