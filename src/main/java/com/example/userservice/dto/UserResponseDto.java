package com.example.userservice.dto;

import java.time.Instant;
import java.time.LocalDate;

public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}