package com.taskflow.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskCompletedEvent(
    UUID taskId,
    String taskTitle,
    UUID projectId,
    UUID completedById,
    LocalDateTime occurredAt
) {
    public static TaskCompletedEvent of(UUID taskId, String taskTitle,
                                        UUID projectId, UUID completedById) {
        return new TaskCompletedEvent(taskId, taskTitle, projectId, completedById,
            LocalDateTime.now());
    }
}
