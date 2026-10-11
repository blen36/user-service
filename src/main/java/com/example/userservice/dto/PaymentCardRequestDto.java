package com.example.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentCardRequestDto(

        @NotBlank(message = "Card number is required")
        @Pattern(
                regexp = "\\d{13,19}",
                message = "Card number must contain from 13 to 19 digits"
        )
        String number,

        @NotBlank(message = "Card holder is required")
        @Size(max = 150, message = "Card holder must contain at most 150 characters")
        String holder,

        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate

) {
}