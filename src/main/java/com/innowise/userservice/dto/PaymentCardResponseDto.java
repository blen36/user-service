package com.innowise.userservice.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentCardResponseDto(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
