package com.taskflow.dto.request;

import com.taskflow.entity.enums.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateTaskRequest(
    @NotBlank(message = "Task title is required")
    @Size(min = 2, max = 200, message = "Title must be 2-200 characters")
    String title,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotNull(message = "Priority is required")
    Priority priority,

    @Future(message = "Due date must be in the future")
    LocalDate dueDate,

    UUID assigneeId,

    Set<UUID> labelIds
) {}
