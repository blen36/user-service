package com.example.userservice.security;

import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceAccessTest {
    @Mock UserRepository users;
    @Mock PaymentCardRepository cards;
    ResourceAccess access;

    @BeforeEach void setUp() { access = new ResourceAccess(users, cards); }

    @Test void adminCanAccessAnyUserAndCard() {
        var admin = auth("15", "ADMIN");
        assertThat(access.canAccessUser(99L, admin)).isTrue();
        assertThat(access.canAccessCard(99L, admin)).isTrue();
        verifyNoInteractions(users, cards);
    }
    @Test void activeUserCanAccessOwnProfile() {
        when(users.existsByIdAndActiveTrue(15L)).thenReturn(true);
        assertThat(access.canAccessUser(15L, auth("15", "USER"))).isTrue();
    }
    @Test void userCannotAccessOtherProfile() {
        assertThat(access.canAccessUser(99L, auth("15", "USER"))).isFalse();
        verifyNoInteractions(users, cards);
    }
    @Test void inactiveUserCannotAccessOwnProfileOrCard() {
        var user = auth("15", "USER");
        assertThat(access.canAccessUser(15L, user)).isFalse();
        assertThat(access.canAccessCard(100L, user)).isFalse();
        verifyNoInteractions(cards);
    }
    @Test void activeUserCanAccessOwnedCard() {
        when(users.existsByIdAndActiveTrue(15L)).thenReturn(true);
        when(cards.existsByIdAndUser_Id(100L, 15L)).thenReturn(true);
        assertThat(access.canAccessCard(100L, auth("15", "USER"))).isTrue();
    }
    @Test void activeUserCannotAccessAnotherCard() {
        when(users.existsByIdAndActiveTrue(15L)).thenReturn(true);
        assertThat(access.canAccessCard(100L, auth("15", "USER"))).isFalse();
    }
    @Test void anonymousCannotAccessResources() {
        assertThat(access.canAccessUser(15L, null)).isFalse();
        assertThat(access.canAccessCard(100L, null)).isFalse();
        verifyNoInteractions(users, cards);
    }
    @Test void nonJwtPrincipalCannotPretendToBeAUser() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated("15", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThat(access.canAccessUser(15L, authentication)).isFalse();
        verifyNoInteractions(users, cards);
    }
    @Test void invalidSubjectCannotAccessResources() {
        assertThat(access.canAccessUser(15L, auth("invalid", "USER"))).isFalse();
        verifyNoInteractions(users, cards);
    }
    @Test void unsupportedRoleCannotAccessResources() {
        assertThat(access.canAccessUser(15L, auth("15", "GUEST"))).isFalse();
        verifyNoInteractions(users, cards);
    }
    private JwtAuthenticationToken auth(String subject, String role) {
        Jwt jwt = Jwt.withTokenValue("unit-test-token").header("alg", "RS256").subject(subject).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
