package com.innowise.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record StatusRequestDto(@NotNull Boolean active) {}
