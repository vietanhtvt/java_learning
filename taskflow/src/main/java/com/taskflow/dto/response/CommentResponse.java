package com.taskflow.dto.response;

import com.taskflow.entity.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    String content,
    UserSummaryResponse author,
    UUID taskId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            UserSummaryResponse.from(comment.getAuthor()),
            comment.getTask().getId(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
