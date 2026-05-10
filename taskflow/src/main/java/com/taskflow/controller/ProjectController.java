package com.taskflow.controller;

import com.taskflow.dto.request.AddMemberRequest;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "List my projects")
    public ResponseEntity<Page<ProjectResponse>> getMyProjects(
        @AuthenticationPrincipal UserDetails user,
        @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {

        UUID userId = extractUserId(user);
        return ResponseEntity.ok(projectService.getMyProjects(userId, pageable));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project details")
    public ResponseEntity<ProjectResponse> getProject(
        @PathVariable UUID projectId,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(projectService.getProject(projectId, extractUserId(user)));
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ProjectResponse> createProject(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal UserDetails user) {

        ProjectResponse response = projectService.createProject(request, extractUserId(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update project")
    public ResponseEntity<ProjectResponse> updateProject(
        @PathVariable UUID projectId,
        @Valid @RequestBody UpdateProjectRequest request,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(projectService.updateProject(projectId, request, extractUserId(user)));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete project")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
        @PathVariable UUID projectId,
        @AuthenticationPrincipal UserDetails user) {

        projectService.deleteProject(projectId, extractUserId(user));
    }

    @PostMapping("/{projectId}/members")
    @Operation(summary = "Add member to project")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(
        @PathVariable UUID projectId,
        @Valid @RequestBody AddMemberRequest request,
        @AuthenticationPrincipal UserDetails user) {

        projectService.addMember(projectId, request, extractUserId(user));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(summary = "Remove member from project")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
        @PathVariable UUID projectId,
        @PathVariable UUID memberId,
        @AuthenticationPrincipal UserDetails user) {

        projectService.removeMember(projectId, memberId, extractUserId(user));
    }

    private UUID extractUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
