package com.taskflow.service;

import com.taskflow.aop.Auditable;
import com.taskflow.config.RedisConfig;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.*;
import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;
import com.taskflow.exception.AccessDeniedException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.kafka.NotificationProducer;
import com.taskflow.kafka.event.TaskAssignedEvent;
import com.taskflow.kafka.event.TaskCompletedEvent;
import com.taskflow.repository.*;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final NotificationProducer notificationProducer;
    private final Counter taskCreatedCounter;
    private final Counter taskCompletedCounter;

    public PageResponse<TaskResponse> getTasksByProject(
        UUID projectId, UUID userId, TaskStatus status, Priority priority,
        UUID assigneeId, Pageable pageable) {

        assertMember(projectId, userId);
        Page<TaskResponse> page = taskRepository.findByProjectWithFilters(
            projectId, status, priority, assigneeId, pageable)
            .map(TaskResponse::from);
        return PageResponse.from(page);
    }

    @Cacheable(value = RedisConfig.CACHE_TASKS, key = "#taskId")
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

        task = taskRepository.save(task);
        taskCreatedCounter.increment();

        // Publish Kafka event if assignee was set
        if (assignee != null) {
            notificationProducer.sendTaskAssigned(
                TaskAssignedEvent.of(task.getId(), task.getTitle(),
                    projectId, assignee.getId(), reporterId));
        }

        return TaskResponse.from(task);
    }

    @Transactional
    @Auditable(action = "UPDATE_TASK")
    @CacheEvict(value = RedisConfig.CACHE_TASKS, key = "#taskId")
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        assertMember(task.getProject().getId(), userId);

        boolean wasAssigneeChanged = request.assigneeId() != null
            && !request.assigneeId().equals(task.getAssignee() != null ? task.getAssignee().getId() : null);

        boolean wasCompleted = request.status() == TaskStatus.DONE
            && task.getStatus() != TaskStatus.DONE;

        if (request.title() != null) task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.dueDate() != null) task.setDueDate(request.dueDate());

        if (request.assigneeId() != null) {
            User newAssignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.assigneeId()));
            task.setAssignee(newAssignee);
        }

        if (request.labelIds() != null) {
            task.setLabels(resolveLabels(request.labelIds(), task.getProject().getId()));
        }

        task = taskRepository.save(task);

        // Publish events after successful save
        if (wasAssigneeChanged && task.getAssignee() != null) {
            notificationProducer.sendTaskAssigned(
                TaskAssignedEvent.of(task.getId(), task.getTitle(),
                    task.getProject().getId(), task.getAssignee().getId(), userId));
        }
        if (wasCompleted) {
            taskCompletedCounter.increment();
            notificationProducer.sendTaskCompleted(
                TaskCompletedEvent.of(task.getId(), task.getTitle(),
                    task.getProject().getId(), userId));
        }

        return TaskResponse.from(task);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_TASKS, key = "#taskId")
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
