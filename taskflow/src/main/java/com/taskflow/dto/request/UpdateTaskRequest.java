package com.taskflow.dto.request;

import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record UpdateTaskRequest(
    @Size(min = 2, max = 200, message = "Title must be 2-200 characters")
    String title,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    TaskStatus status,

    Priority priority,

    LocalDate dueDate,

    UUID assigneeId,

    Set<UUID> labelIds
) {}
