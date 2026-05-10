package com.taskflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    String name,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description
) {}
