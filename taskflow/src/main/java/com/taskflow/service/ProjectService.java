package com.taskflow.service;

import com.taskflow.aop.Auditable;
import com.taskflow.config.RedisConfig;
import com.taskflow.dto.request.AddMemberRequest;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import com.taskflow.entity.UserProject;
import com.taskflow.entity.enums.ProjectRole;
import com.taskflow.entity.enums.ProjectStatus;
import com.taskflow.exception.AccessDeniedException;
import com.taskflow.exception.BusinessException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.UserProjectRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectRepository userProjectRepository;

    public Page<ProjectResponse> getMyProjects(UUID userId, Pageable pageable) {
        return projectRepository.findProjectsByMemberId(userId, pageable)
            .map(ProjectResponse::from);
    }

    @Cacheable(value = RedisConfig.CACHE_PROJECTS, key = "#projectId")
    public ProjectResponse getProject(UUID projectId, UUID userId) {
        Project project = findProjectOrThrow(projectId);
        assertMember(projectId, userId);
        return ProjectResponse.from(project);
    }

    @Transactional
    @Auditable(action = "CREATE_PROJECT")
    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {
        User owner = findUserOrThrow(ownerId);

        Project project = Project.builder()
            .name(request.name())
            .description(request.description())
            .owner(owner)
            .status(ProjectStatus.ACTIVE)
            .build();

        project = projectRepository.save(project);

        UserProject membership = UserProject.builder()
            .user(owner)
            .project(project)
            .role(ProjectRole.OWNER)
            .build();
        userProjectRepository.save(membership);

        return ProjectResponse.from(project);
    }

    @Transactional
    @Auditable(action = "UPDATE_PROJECT")
    @CacheEvict(value = RedisConfig.CACHE_PROJECTS, key = "#projectId")
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UUID userId) {
        Project project = findProjectOrThrow(projectId);
        assertOwner(projectId, userId);

        if (request.name() != null) project.setName(request.name());
        if (request.description() != null) project.setDescription(request.description());
        if (request.status() != null) project.setStatus(request.status());

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    @Auditable(action = "DELETE_PROJECT")
    @CacheEvict(value = RedisConfig.CACHE_PROJECTS, key = "#projectId")
    public void deleteProject(UUID projectId, UUID userId) {
        findProjectOrThrow(projectId);
        assertOwner(projectId, userId);
        projectRepository.deleteById(projectId);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_PROJECTS, key = "#projectId")
    public void addMember(UUID projectId, AddMemberRequest request, UUID requesterId) {
        findProjectOrThrow(projectId);
        assertOwner(projectId, requesterId);

        if (userProjectRepository.findByUserIdAndProjectId(request.userId(), projectId).isPresent()) {
            throw new BusinessException("User is already a member of this project");
        }

        User user = findUserOrThrow(request.userId());
        Project project = findProjectOrThrow(projectId);

        UserProject membership = UserProject.builder()
            .user(user)
            .project(project)
            .role(request.role())
            .build();
        userProjectRepository.save(membership);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_PROJECTS, key = "#projectId")
    public void removeMember(UUID projectId, UUID memberId, UUID requesterId) {
        findProjectOrThrow(projectId);
        assertOwner(projectId, requesterId);

        if (projectRepository.existsByIdAndOwnerId(projectId, memberId)) {
            throw new BusinessException("Cannot remove project owner from members");
        }

        userProjectRepository.deleteByUserIdAndProjectId(memberId, projectId);
    }

    // --- helpers ---

    private Project findProjectOrThrow(UUID id) {
        return projectRepository.findByIdWithOwner(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void assertOwner(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, userId)) {
            throw new AccessDeniedException("Only the project owner can perform this action");
        }
    }

    private void assertMember(UUID projectId, UUID userId) {
        if (!projectRepository.isMember(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }
}
