package com.example.userservice.security;

import com.example.userservice.entity.User;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class UserAccess {

    private final UserRepository users;
    private final PaymentCardRepository cards;

    public UserAccess(
            UserRepository users,
            PaymentCardRepository cards
    ) {
        this.users = users;
        this.cards = cards;
    }

    public boolean canAccessUser(
            Authentication authentication,
            String requestedUserId
    ) {
        if (isAdmin(authentication)) {
            return true;
        }

        Long currentUserId = currentUserId(authentication);
        Long targetUserId = parseId(requestedUserId);

        return currentUserId != null
                && currentUserId.equals(targetUserId)
                && isActive(currentUserId);
    }

    public boolean canAccessCard(
            Authentication authentication,
            String requestedCardId
    ) {
        if (isAdmin(authentication)) {
            return true;
        }

        Long currentUserId = currentUserId(authentication);
        Long cardId = parseId(requestedCardId);

        return currentUserId != null
                && cardId != null
                && isActive(currentUserId)
                && cards.existsByIdAndUser_Id(cardId, currentUserId);
    }

    private boolean isActive(Long userId) {
        return users.findById(userId)
                .map(User::isActive)
                .orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return parseId(jwt.getSubject());
    }

    private Long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}