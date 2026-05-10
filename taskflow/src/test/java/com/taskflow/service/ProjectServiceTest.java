package com.taskflow.service;

import com.taskflow.dto.request.AddMemberRequest;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService")
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock UserProjectRepository userProjectRepository;

    @InjectMocks ProjectService projectService;

    UUID ownerId;
    UUID projectId;
    User owner;
    Project project;

    @BeforeEach
    void setUp() {
        ownerId    = UUID.randomUUID();
        projectId  = UUID.randomUUID();

        owner = User.builder().username("alice").email("alice@test.com").build();
        owner.setId(ownerId);

        project = Project.builder().name("My Project").owner(owner).status(ProjectStatus.ACTIVE).build();
        project.setId(projectId);
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("createProject()")
    class CreateProject {

        @Test
        @DisplayName("성공 — owner auto-added as OWNER membership")
        void success_ownerMembershipCreated() {
            var request = new CreateProjectRequest("My Project", "desc");
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(projectRepository.save(any(Project.class))).willAnswer(inv -> {
                Project p = inv.getArgument(0);
                p.setId(projectId);
                return p;
            });

            var response = projectService.createProject(request, ownerId);

            assertThat(response.name()).isEqualTo("My Project");

            var captor = ArgumentCaptor.forClass(UserProject.class);
            then(userProjectRepository).should().save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(ProjectRole.OWNER);
        }

        @Test
        @DisplayName("실패 — owner user not found → ResourceNotFoundException")
        void fail_ownerNotFound() {
            given(userRepository.findById(ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                projectService.createProject(new CreateProjectRequest("P", null), ownerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        }
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("updateProject()")
    class UpdateProject {

        @Test
        @DisplayName("성공 — owner can update name and status")
        void success() {
            var request = new UpdateProjectRequest("New Name", null, ProjectStatus.ARCHIVED);
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(projectRepository.existsByIdAndOwnerId(projectId, ownerId)).willReturn(true);
            given(projectRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var response = projectService.updateProject(projectId, request, ownerId);

            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
        }

        @Test
        @DisplayName("실패 — non-owner → AccessDeniedException")
        void fail_nonOwner() {
            var otherId = UUID.randomUUID();
            var request = new UpdateProjectRequest("Hack", null, null);
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(projectRepository.existsByIdAndOwnerId(projectId, otherId)).willReturn(false);

            assertThatThrownBy(() -> projectService.updateProject(projectId, request, otherId))
                .isInstanceOf(AccessDeniedException.class);

            then(projectRepository).should(never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("addMember()")
    class AddMember {

        @Test
        @DisplayName("실패 — user already a member → BusinessException")
        void fail_alreadyMember() {
            var newUserId = UUID.randomUUID();
            var request   = new AddMemberRequest(newUserId, ProjectRole.MEMBER);

            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(projectRepository.existsByIdAndOwnerId(projectId, ownerId)).willReturn(true);
            given(userProjectRepository.findByUserIdAndProjectId(newUserId, projectId))
                .willReturn(Optional.of(new UserProject()));

            assertThatThrownBy(() -> projectService.addMember(projectId, request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
        }

        @Test
        @DisplayName("실패 — requester is not owner → AccessDeniedException")
        void fail_nonOwner() {
            var nonOwner  = UUID.randomUUID();
            var request   = new AddMemberRequest(UUID.randomUUID(), ProjectRole.MEMBER);

            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(projectRepository.existsByIdAndOwnerId(projectId, nonOwner)).willReturn(false);

            assertThatThrownBy(() -> projectService.addMember(projectId, request, nonOwner))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteProject()")
    class DeleteProject {

        @Test
        @DisplayName("성공 — owner deletes project")
        void success() {
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(projectRepository.existsByIdAndOwnerId(projectId, ownerId)).willReturn(true);

            assertThatCode(() -> projectService.deleteProject(projectId, ownerId))
                .doesNotThrowAnyException();

            then(projectRepository).should().deleteById(projectId);
        }

        @Test
        @DisplayName("실패 — project not found → ResourceNotFoundException")
        void fail_notFound() {
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(projectId, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
