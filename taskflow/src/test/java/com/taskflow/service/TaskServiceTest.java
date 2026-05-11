package com.taskflow.service;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock LabelRepository labelRepository;
    @Mock NotificationProducer notificationProducer;
    @Mock Counter taskCreatedCounter;
    @Mock Counter taskCompletedCounter;

    @InjectMocks TaskService taskService;

    UUID projectId;
    UUID reporterId;
    UUID assigneeId;
    UUID taskId;

    User reporter;
    User assignee;
    Project project;
    Task task;

    @BeforeEach
    void setUp() {
        projectId  = UUID.randomUUID();
        reporterId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        taskId     = UUID.randomUUID();

        reporter = User.builder().username("reporter").fullName("Reporter").build();
        reporter.setId(reporterId);

        assignee = User.builder().username("assignee").fullName("Assignee").build();
        assignee.setId(assigneeId);

        project = Project.builder().name("Test Project").owner(reporter).build();
        project.setId(projectId);

        task = Task.builder()
            .title("Fix bug")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .project(project)
            .reporter(reporter)
            .build();
        task.setId(taskId);
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("성공 — without assignee, no Kafka event")
        void success_noAssignee() {
            var request = new CreateTaskRequest("Fix bug", "desc", Priority.HIGH, null, null, null);
            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(userRepository.findById(reporterId)).willReturn(Optional.of(reporter));
            given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(taskId);
                return t;
            });

            var response = taskService.createTask(projectId, request, reporterId);

            assertThat(response.title()).isEqualTo("Fix bug");
            then(notificationProducer).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 — with assignee → publishes TaskAssignedEvent")
        void success_withAssignee_publishesEvent() {
            var request = new CreateTaskRequest("Fix bug", null, Priority.HIGH, null, assigneeId, null);
            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(userRepository.findById(reporterId)).willReturn(Optional.of(reporter));
            given(userRepository.findById(assigneeId)).willReturn(Optional.of(assignee));
            given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(taskId);
                t.setAssignee(assignee);
                return t;
            });

            taskService.createTask(projectId, request, reporterId);

            var captor = ArgumentCaptor.forClass(TaskAssignedEvent.class);
            then(notificationProducer).should().sendTaskAssigned(captor.capture());
            assertThat(captor.getValue().assigneeId()).isEqualTo(assigneeId);
            assertThat(captor.getValue().assignedById()).isEqualTo(reporterId);
        }

        @Test
        @DisplayName("실패 — not a project member → AccessDeniedException")
        void fail_notMember() {
            var request = new CreateTaskRequest("Fix bug", null, Priority.LOW, null, null, null);
            given(projectRepository.isMember(projectId, reporterId)).willReturn(false);

            assertThatThrownBy(() -> taskService.createTask(projectId, request, reporterId))
                .isInstanceOf(AccessDeniedException.class);

            then(taskRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패 — project not found → ResourceNotFoundException")
        void fail_projectNotFound() {
            var request = new CreateTaskRequest("Fix bug", null, Priority.LOW, null, null, null);
            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(projectId, request, reporterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
        }
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("status → DONE publishes TaskCompletedEvent")
        void statusToDone_publishesCompletedEvent() {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setAssignee(assignee);
            var request = new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null, null);

            given(taskRepository.findByIdWithDetails(taskId)).willReturn(Optional.of(task));
            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);
            given(taskRepository.save(any())).willReturn(task);

            taskService.updateTask(taskId, request, reporterId);

            var captor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
            then(notificationProducer).should().sendTaskCompleted(captor.capture());
            assertThat(captor.getValue().taskId()).isEqualTo(taskId);
        }

        @Test
        @DisplayName("assignee 변경 → publishes TaskAssignedEvent")
        void assigneeChange_publishesAssignedEvent() {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setAssignee(reporter);
            var newAssigneeId = UUID.randomUUID();
            var newAssignee = User.builder().username("new").build();
            newAssignee.setId(newAssigneeId);

            var request = new UpdateTaskRequest(null, null, null, null, null, newAssigneeId, null);
            given(taskRepository.findByIdWithDetails(taskId)).willReturn(Optional.of(task));
            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);
            given(userRepository.findById(newAssigneeId)).willReturn(Optional.of(newAssignee));
            given(taskRepository.save(any())).willAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setAssignee(newAssignee);
                return t;
            });

            taskService.updateTask(taskId, request, reporterId);

            var captor = ArgumentCaptor.forClass(TaskAssignedEvent.class);
            then(notificationProducer).should().sendTaskAssigned(captor.capture());
            assertThat(captor.getValue().assigneeId()).isEqualTo(newAssigneeId);
        }

        @Test
        @DisplayName("task not found → ResourceNotFoundException")
        void taskNotFound() {
            given(taskRepository.findByIdWithDetails(taskId)).willReturn(Optional.empty());
            var request = new UpdateTaskRequest(null, null, null, null, null, null, null);

            assertThatThrownBy(() -> taskService.updateTask(taskId, request, reporterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
        }
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteTask()")
    class DeleteTask {

        @Test
        @DisplayName("성공 — member can delete")
        void success() {
            given(taskRepository.findByIdWithDetails(taskId)).willReturn(Optional.of(task));
            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);

            assertThatCode(() -> taskService.deleteTask(taskId, reporterId))
                .doesNotThrowAnyException();

            then(taskRepository).should().delete(task);
        }

        @Test
        @DisplayName("실패 — non-member cannot delete")
        void fail_notMember() {
            given(taskRepository.findByIdWithDetails(taskId)).willReturn(Optional.of(task));
            given(projectRepository.isMember(projectId, reporterId)).willReturn(false);

            assertThatThrownBy(() -> taskService.deleteTask(taskId, reporterId))
                .isInstanceOf(AccessDeniedException.class);

            then(taskRepository).should(never()).delete(any());
        }
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getTask()")
    class GetTask {

        @Test
        @DisplayName("task not found → ResourceNotFoundException")
        void taskNotFound() {
            given(taskRepository.findByIdWithDetails(taskId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTask(taskId, reporterId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
