package com.taskflow.controller;

import com.taskflow.AbstractIntegrationTest;
import com.taskflow.dto.request.*;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaskController IT")
class TaskControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    HttpHeaders authHeaders;
    UUID projectId;

    @BeforeEach
    void setUpUserAndProject() {
        // Register & login
        var username = "task_user_" + UUID.randomUUID().toString().substring(0, 6);
        var auth = register(username, username + "@test.com");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(auth.accessToken());
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        // Create a project
        var projectReq = new CreateProjectRequest("IT Project", "For integration tests");
        var projResp = restTemplate.exchange(
            "/api/projects", HttpMethod.POST,
            new HttpEntity<>(projectReq, authHeaders), ProjectResponse.class);
        assertThat(projResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        projectId = projResp.getBody().id();
    }

    // ---- helpers -----------------------------------------------------------

    AuthResponse register(String username, String email) {
        var body = new RegisterRequest(username, email, "Password1!", "IT User");
        return restTemplate.postForEntity("/api/auth/register", body, AuthResponse.class).getBody();
    }

    TaskResponse createTask(String title) {
        var req = new CreateTaskRequest(title, "desc", Priority.HIGH,
            LocalDate.now().plusDays(7), null, null);
        var resp = restTemplate.exchange(
            "/api/projects/" + projectId + "/tasks", HttpMethod.POST,
            new HttpEntity<>(req, authHeaders), TaskResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    // ---- tests -------------------------------------------------------------

    @Test
    @DisplayName("create task → get task → fields match")
    void createAndGetTask() {
        var created = createTask("Fix Login Bug");

        var resp = restTemplate.exchange(
            "/api/tasks/" + created.id(), HttpMethod.GET,
            new HttpEntity<>(authHeaders), TaskResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().title()).isEqualTo("Fix Login Bug");
        assertThat(resp.getBody().status()).isEqualTo(TaskStatus.TODO);
        assertThat(resp.getBody().priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("update task status → response reflects new status")
    void updateTask_status() {
        var task = createTask("Implement Feature");

        var update = new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null, null);
        var resp = restTemplate.exchange(
            "/api/tasks/" + task.id(), HttpMethod.PUT,
            new HttpEntity<>(update, authHeaders), TaskResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("delete task → subsequent GET returns 404")
    void deleteTask() {
        var task = createTask("Task to Delete");

        var deleteResp = restTemplate.exchange(
            "/api/tasks/" + task.id(), HttpMethod.DELETE,
            new HttpEntity<>(authHeaders), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResp = restTemplate.exchange(
            "/api/tasks/" + task.id(), HttpMethod.GET,
            new HttpEntity<>(authHeaders), String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("list tasks in project → paginated result")
    void listTasks_pagination() {
        createTask("Task A");
        createTask("Task B");
        createTask("Task C");

        var resp = restTemplate.exchange(
            "/api/projects/" + projectId + "/tasks?page=0&size=2",
            HttpMethod.GET, new HttpEntity<>(authHeaders),
            new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).containsKey("content");
        assertThat(body).containsKey("totalElements");
        assertThat((Integer) body.get("totalElements")).isGreaterThanOrEqualTo(3);
        assertThat((Integer) body.get("size")).isEqualTo(2);
    }

    @Test
    @DisplayName("add comment → list comments")
    void addAndListComments() {
        var task = createTask("Commented Task");

        var commentReq = new CreateCommentRequest("Great progress!");
        var addResp = restTemplate.exchange(
            "/api/tasks/" + task.id() + "/comments", HttpMethod.POST,
            new HttpEntity<>(commentReq, authHeaders), String.class);
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var listResp = restTemplate.exchange(
            "/api/tasks/" + task.id() + "/comments", HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) listResp.getBody().get("totalElements")).isEqualTo(1);
    }

    @Test
    @DisplayName("non-member cannot access project tasks → 403")
    void nonMember_forbidden() {
        // Create a different user
        var other = register("other_" + UUID.randomUUID().toString().substring(0, 6), "other@x.com");
        var otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(other.accessToken());

        var resp = restTemplate.exchange(
            "/api/projects/" + projectId + "/tasks", HttpMethod.GET,
            new HttpEntity<>(otherHeaders), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
