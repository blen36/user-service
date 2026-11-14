package com.innowise.userservice.controller;

import com.innowise.userservice.dto.OrderUserProfile;
import com.innowise.userservice.repository.OrderUserLookup;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-users")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class OrderUserLookupController {
    private final OrderUserLookup users;

    public OrderUserLookupController(OrderUserLookup users) {
        this.users = users;
    }

    @GetMapping(params = {"email", "!id"})
    public ResponseEntity<OrderUserProfile> byEmail(@RequestParam @NotBlank @Email @Size(max = 320) String email,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.of(users.byEmail(email, Long.parseLong(jwt.getSubject()),
                "ADMIN".equals(jwt.getClaimAsString("role"))));
    }

    @GetMapping(params = {"id", "!email"})
    public ResponseEntity<OrderUserProfile> byId(@RequestParam @Positive long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.of(users.byId(id, Long.parseLong(jwt.getSubject()),
                "ADMIN".equals(jwt.getClaimAsString("role"))));
    }
}
