package com.taskflow.security;

import com.taskflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Spring Security expression bean used via @PreAuthorize("@projectSecurity.isOwner(#id, authentication)").
 */
@Component("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurity {

    private final ProjectRepository projectRepository;

    public boolean isOwner(UUID projectId, org.springframework.security.core.Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return projectRepository.existsByIdAndOwnerId(projectId, userId);
    }

    public boolean isMember(UUID projectId, org.springframework.security.core.Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return projectRepository.isMember(projectId, userId);
    }
}
