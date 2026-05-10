package com.taskflow.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskAssignedEvent(
    UUID taskId,
    String taskTitle,
    UUID projectId,
    UUID assigneeId,
    UUID assignedById,
    LocalDateTime occurredAt
) {
    public static TaskAssignedEvent of(UUID taskId, String taskTitle,
                                       UUID projectId, UUID assigneeId, UUID assignedById) {
        return new TaskAssignedEvent(taskId, taskTitle, projectId, assigneeId, assignedById,
            LocalDateTime.now());
    }
}
