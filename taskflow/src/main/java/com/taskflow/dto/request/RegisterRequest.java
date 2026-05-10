package com.taskflow.dto.request;

import com.taskflow.validation.UniqueEmail;
import com.taskflow.validation.UniqueUsername;
import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, and underscores")
    @UniqueUsername
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @UniqueEmail
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    String password,

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    String fullName
) {}
