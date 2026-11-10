package com.innowise.userservice.controller;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('REGISTRATION')")
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService users;

    public InternalUserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> create(
            @Valid @RequestBody UserRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(users.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> rollback(@PathVariable Long id) {
        try {
            users.delete(id);
        } catch (ResourceNotFoundException ignored) {
            // A repeated rollback is successful if the user is already absent.
        }

        return ResponseEntity.noContent().build();
    }
}
