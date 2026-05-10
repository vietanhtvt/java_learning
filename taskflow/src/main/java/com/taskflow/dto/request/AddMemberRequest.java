package com.taskflow.dto.request;

import com.taskflow.entity.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotNull(message = "Role is required")
    ProjectRole role
) {}
