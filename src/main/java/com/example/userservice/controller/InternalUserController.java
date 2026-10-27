package com.example.userservice.controller;

import com.example.userservice.dto.UserRequestDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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