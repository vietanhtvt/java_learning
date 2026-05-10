package com.taskflow.dto.response;

import com.taskflow.entity.Project;
import com.taskflow.entity.enums.ProjectStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String description,
    ProjectStatus status,
    UserSummaryResponse owner,
    int memberCount,
    int taskCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getStatus(),
            UserSummaryResponse.from(project.getOwner()),
            project.getMembers().size(),
            project.getTasks().size(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
