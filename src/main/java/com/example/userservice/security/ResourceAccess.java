package com.example.userservice.security;

import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("resourceAccess")
public class ResourceAccess {
    private final UserRepository users;
    private final PaymentCardRepository cards;

    public ResourceAccess(UserRepository users, PaymentCardRepository cards) {
        this.users = users;
        this.cards = cards;
    }

    public boolean canAccessUser(Long targetUserId, Authentication authentication) {
        if (isAdmin(authentication)) return true;
        Long currentUserId = currentUserId(authentication);
        return currentUserId != null && currentUserId.equals(targetUserId)
                && users.existsByIdAndActiveTrue(currentUserId);
    }

    public boolean canAccessCard(Long cardId, Authentication authentication) {
        if (isAdmin(authentication)) return true;
        Long currentUserId = currentUserId(authentication);
        return currentUserId != null && users.existsByIdAndActiveTrue(currentUserId)
                && cards.existsByIdAndUser_Id(cardId, currentUserId);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_USER".equals(a.getAuthority()))
                || !(authentication.getPrincipal() instanceof Jwt jwt)) return null;
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
