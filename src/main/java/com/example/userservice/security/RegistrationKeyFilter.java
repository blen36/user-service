package com.example.userservice.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RegistrationKeyFilter extends OncePerRequestFilter {

    private final byte[] expectedKey;
    private final SecurityErrorWriter errors;

    public RegistrationKeyFilter(
            String registrationKey,
            SecurityErrorWriter errors
    ) {
        if (registrationKey == null || registrationKey.length() < 32) {
            throw new IllegalArgumentException(
                    "Registration key must contain at least 32 characters"
            );
        }

        this.expectedKey = registrationKey.getBytes(StandardCharsets.UTF_8);
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String suppliedKey = request.getHeader("X-Registration-Key");

        if (suppliedKey == null || !MessageDigest.isEqual(
                expectedKey,
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        )) {
            errors.write(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "A valid registration key is required"
            );
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "api-gateway",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GATEWAY"))
        );

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }
}