package com.taskflow.dto.request;

import com.taskflow.entity.enums.ProjectStatus;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    String name,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    ProjectStatus status
) {}
