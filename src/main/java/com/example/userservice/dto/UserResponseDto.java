package com.example.userservice.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<PaymentCardResponseDto> paymentCards
) implements Serializable {
}