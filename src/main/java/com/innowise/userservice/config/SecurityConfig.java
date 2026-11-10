package com.innowise.userservice.config;

import com.innowise.userservice.security.RegistrationKeyFilter;
import com.innowise.userservice.security.SecurityErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ACCESS_DENIED_MESSAGE = "Access denied";

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurity(
            HttpSecurity http,
            SecurityErrorWriter errors,
            @Value("${user.registration-key}") String key
    ) throws Exception {
        configureCommon(http, errors);

        var paths = PathPatternRequestMatcher.withDefaults();
        RequestMatcher registrationEndpoint = new OrRequestMatcher(
                paths.matcher(HttpMethod.POST, "/api/users"),
                paths.matcher(HttpMethod.DELETE, "/api/users/{id}"));
        return http
                .securityMatcher(new OrRequestMatcher(paths.matcher("/internal/**"),
                        request -> request.getHeader("X-Registration-Key") != null && registrationEndpoint.matches(request)))
                .authorizeHttpRequests(rules -> rules
                        .anyRequest().hasRole("REGISTRATION")
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
            SecurityErrorWriter errors
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
                        .anyRequest().authenticated()
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
                                        ACCESS_DENIED_MESSAGE
                                )
                        )
                )
                .build();
    }

    private void configureCommon(
            HttpSecurity http,
            SecurityErrorWriter errors
    ) {
        // Authentication uses explicit Authorization or X-Registration-Key headers.
        // No cookie-based authentication or HTTP Basic is used.
        // Reassess CSRF protection if cookie-based authentication is introduced.
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
                                        ACCESS_DENIED_MESSAGE
                                )
                        )
                );
    }
}
