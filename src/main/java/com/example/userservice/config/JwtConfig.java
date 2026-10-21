package com.example.userservice.config;

import com.example.userservice.security.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import java.io.IOException;

@Configuration
public class JwtConfig {
    @Bean
    public JwtDecoder jwtDecoder(@Value("${auth.jwt.public-key}") Resource publicKey,
                                 @Value("${auth.jwt.issuer}") String issuer,
                                 @Value("${auth.jwt.audience}") String audience) throws IOException {
        return JwtSupport.decoder(JwtSupport.readPublicKey(publicKey), issuer, audience, "access");
    }
}
