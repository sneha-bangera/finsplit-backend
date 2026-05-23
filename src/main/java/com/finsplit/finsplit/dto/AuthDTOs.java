package com.finsplit.finsplit.dto;

import jakarta.validation.constraints.*;

public class AuthDTOs {

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 50, message = "Name must be 2–50 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record AuthResponse(
            String token,
            String email,
            String name
    ) {}

    public record UserProfileResponse(
            String id,
            String name,
            String email,
            String createdAt
    ) {}
}
