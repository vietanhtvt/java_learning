package com.taskflow.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentAddedEvent(
    UUID commentId,
    UUID taskId,
    String taskTitle,
    UUID projectId,
    UUID authorId,
    String authorName,
    String contentPreview,
    LocalDateTime occurredAt
) {
    public static CommentAddedEvent of(UUID commentId, UUID taskId, String taskTitle,
                                       UUID projectId, UUID authorId, String authorName,
                                       String content) {
        String preview = content.length() > 100 ? content.substring(0, 100) + "…" : content;
        return new CommentAddedEvent(commentId, taskId, taskTitle, projectId,
            authorId, authorName, preview, LocalDateTime.now());
    }
}
