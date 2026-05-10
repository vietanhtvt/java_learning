package com.taskflow.service;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.*;
import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;
import com.taskflow.exception.AccessDeniedException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;

    public PageResponse<TaskResponse> getTasksByProject(
        UUID projectId, UUID userId, TaskStatus status, Priority priority,
        UUID assigneeId, Pageable pageable) {

        assertMember(projectId, userId);
        Page<TaskResponse> page = taskRepository.findByProjectWithFilters(
            projectId, status, priority, assigneeId, pageable)
            .map(TaskResponse::from);
        return PageResponse.from(page);
    }

    public TaskResponse getTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        assertMember(task.getProject().getId(), userId);
        return TaskResponse.from(task);
    }

    public List<TaskResponse> getOverdueTasks(UUID projectId, UUID userId) {
        assertMember(projectId, userId);
        return taskRepository.findOverdueTasksByProject(projectId, LocalDate.now())
            .stream().map(TaskResponse::from).collect(Collectors.toList());
    }

    public List<TaskResponse> getMyTasks(UUID userId) {
        return taskRepository.findActiveTasksByAssignee(userId)
            .stream().map(TaskResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request, UUID reporterId) {
        assertMember(projectId, reporterId);

        Project project = projectRepository.findByIdWithOwner(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.assigneeId()));
        }

        Set<Label> labels = resolveLabels(request.labelIds(), projectId);

        Task task = Task.builder()
            .title(request.title())
            .description(request.description())
            .priority(request.priority())
            .dueDate(request.dueDate())
            .project(project)
            .reporter(reporter)
            .assignee(assignee)
            .labels(labels)
            .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        assertMember(task.getProject().getId(), userId);

        if (request.title() != null) task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.dueDate() != null) task.setDueDate(request.dueDate());

        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.assigneeId()));
            task.setAssignee(assignee);
        }

        if (request.labelIds() != null) {
            task.setLabels(resolveLabels(request.labelIds(), task.getProject().getId()));
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        assertMember(task.getProject().getId(), userId);
        taskRepository.delete(task);
    }

    // --- helpers ---

    private Task findTaskOrThrow(UUID id) {
        return taskRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private Set<Label> resolveLabels(Set<UUID> labelIds, UUID projectId) {
        if (labelIds == null || labelIds.isEmpty()) return new HashSet<>();
        return labelIds.stream()
            .map(id -> labelRepository.findById(id)
                .filter(l -> l.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Label", id)))
            .collect(Collectors.toSet());
    }

    private void assertMember(UUID projectId, UUID userId) {
        if (!projectRepository.isMember(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }
}
