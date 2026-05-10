package com.taskflow.dto.response;

import com.taskflow.entity.Notification;
import com.taskflow.entity.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String message,
    NotificationType type,
    boolean read,
    UUID taskId,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getTitle(),
            n.getMessage(),
            n.getType(),
            n.isRead(),
            n.getTask() != null ? n.getTask().getId() : null,
            n.getCreatedAt()
        );
    }
}
