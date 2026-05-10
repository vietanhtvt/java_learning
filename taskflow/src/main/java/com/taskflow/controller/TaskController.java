package com.taskflow.controller;

import com.taskflow.dto.request.CreateCommentRequest;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.CommentResponse;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;
import com.taskflow.service.CommentService;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;
    private final CommentService commentService;

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "List tasks in a project")
    public ResponseEntity<PageResponse<TaskResponse>> getTasks(
        @PathVariable UUID projectId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) Priority priority,
        @RequestParam(required = false) UUID assigneeId,
        @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(
            taskService.getTasksByProject(projectId, extractUserId(user), status, priority, assigneeId, pageable));
    }

    @GetMapping("/projects/{projectId}/tasks/overdue")
    @Operation(summary = "List overdue tasks in a project")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks(
        @PathVariable UUID projectId,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(taskService.getOverdueTasks(projectId, extractUserId(user)));
    }

    @GetMapping("/tasks/my")
    @Operation(summary = "List my assigned tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(taskService.getMyTasks(extractUserId(user)));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get task details")
    public ResponseEntity<TaskResponse> getTask(
        @PathVariable UUID taskId,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(taskService.getTask(taskId, extractUserId(user)));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Create a task in a project")
    public ResponseEntity<TaskResponse> createTask(
        @PathVariable UUID projectId,
        @Valid @RequestBody CreateTaskRequest request,
        @AuthenticationPrincipal UserDetails user) {

        TaskResponse response = taskService.createTask(projectId, request, extractUserId(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskResponse> updateTask(
        @PathVariable UUID taskId,
        @Valid @RequestBody UpdateTaskRequest request,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(taskService.updateTask(taskId, request, extractUserId(user)));
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Delete a task")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
        @PathVariable UUID taskId,
        @AuthenticationPrincipal UserDetails user) {

        taskService.deleteTask(taskId, extractUserId(user));
    }

    // --- Comment endpoints ---

    @GetMapping("/tasks/{taskId}/comments")
    @Operation(summary = "List task comments")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
        @PathVariable UUID taskId,
        @PageableDefault(size = 50, sort = "createdAt") Pageable pageable,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(commentService.getComments(taskId, extractUserId(user), pageable));
    }

    @PostMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<CommentResponse> addComment(
        @PathVariable UUID taskId,
        @Valid @RequestBody CreateCommentRequest request,
        @AuthenticationPrincipal UserDetails user) {

        CommentResponse response = commentService.addComment(taskId, request, extractUserId(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
        @PathVariable UUID commentId,
        @AuthenticationPrincipal UserDetails user) {

        commentService.deleteComment(commentId, extractUserId(user));
    }

    private UUID extractUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
