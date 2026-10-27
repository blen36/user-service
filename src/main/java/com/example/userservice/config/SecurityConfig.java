package com.example.userservice.config;

import com.example.userservice.security.RegistrationKeyFilter;
import com.example.userservice.security.SecurityErrorWriter;
import com.example.userservice.security.UserAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurity(
            HttpSecurity http,
            SecurityErrorWriter errors,
            @Value("${user.registration-key}") String key
    ) throws Exception {

        configureCommon(http, errors);

        return http
                .securityMatcher("/internal/**")
                .authorizeHttpRequests(rules -> rules
                        .anyRequest().hasRole("GATEWAY")
                )
                .addFilterBefore(
                        new RegistrationKeyFilter(key, errors),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurity(
            HttpSecurity http,
            JwtDecoder decoder,
            SecurityErrorWriter errors,
            UserAccess access
    ) throws Exception {

        configureCommon(http, errors);

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return http
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.GET, "/api/ping")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/{userId}"
                        )
                        .access((authentication, context) ->
                                new AuthorizationDecision(
                                        access.canAccessUser(
                                                authentication.get(),
                                                context.getVariables().get("userId")
                                        )
                                )
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/users/{userId}"
                        )
                        .access((authentication, context) ->
                                new AuthorizationDecision(
                                        access.canAccessUser(
                                                authentication.get(),
                                                context.getVariables().get("userId")
                                        )
                                )
                        )

                        .requestMatchers("/api/v1/users/{userId}/cards")
                        .access((authentication, context) ->
                                new AuthorizationDecision(
                                        access.canAccessUser(
                                                authentication.get(),
                                                context.getVariables().get("userId")
                                        )
                                )
                        )

                        .requestMatchers(
                                "/api/v1/cards/{cardId}",
                                "/api/v1/cards/{cardId}/active"
                        )
                        .access((authentication, context) ->
                                new AuthorizationDecision(
                                        access.canAccessCard(
                                                authentication.get(),
                                                context.getVariables().get("cardId")
                                        )
                                )
                        )

                        .requestMatchers(
                                "/api/v1/users",
                                "/api/v1/users/**",
                                "/api/v1/cards"
                        )
                        .hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt
                                .decoder(decoder)
                                .jwtAuthenticationConverter(converter)
                        )
                        .authenticationEntryPoint((request, response, exception) ->
                                errors.write(
                                        request,
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Invalid or expired access token"
                                )
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                errors.write(
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "Access denied"
                                )
                        )
                )
                .build();
    }

    private void configureCommon(
            HttpSecurity http,
            SecurityErrorWriter errors
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(configuration -> configuration
                        .authenticationEntryPoint((request, response, exception) ->
                                errors.write(
                                        request,
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "A valid access token is required"
                                )
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                errors.write(
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "Access denied"
                                )
                        )
                );
    }
}