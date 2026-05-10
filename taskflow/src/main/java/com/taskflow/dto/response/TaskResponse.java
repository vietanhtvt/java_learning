package com.taskflow.dto.response;

import com.taskflow.entity.Task;
import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    Priority priority,
    LocalDate dueDate,
    UUID projectId,
    String projectName,
    UserSummaryResponse assignee,
    UserSummaryResponse reporter,
    Set<LabelResponse> labels,
    long commentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            task.getProject().getId(),
            task.getProject().getName(),
            task.getAssignee() != null ? UserSummaryResponse.from(task.getAssignee()) : null,
            task.getReporter() != null ? UserSummaryResponse.from(task.getReporter()) : null,
            task.getLabels().stream().map(LabelResponse::from).collect(Collectors.toSet()),
            task.getComments().size(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
