package com.taskflow.service;

import com.taskflow.AbstractIntegrationTest;
import com.taskflow.config.RedisConfig;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.entity.enums.ProjectStatus;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cache IT — Redis @Cacheable / @CacheEvict")
class CacheIT extends AbstractIntegrationTest {

    @Autowired ProjectService  projectService;
    @Autowired UserRepository  userRepository;
    @Autowired PasswordEncoder  passwordEncoder;
    @Autowired CacheManager    cacheManager;

    // ---- helpers -----------------------------------------------------------

    UUID createUserAndAuthenticate(String username) {
        var request = new RegisterRequest(username, username + "@test.com", "Password1!", "Test User");
        com.taskflow.entity.User user = com.taskflow.entity.User.builder()
            .username(username)
            .email(username + "@test.com")
            .password(passwordEncoder.encode("Password1!"))
            .fullName("Test User")
            .build();
        user = userRepository.save(user);

        // Set Spring Security context so AuditorAware works
        var auth = new UsernamePasswordAuthenticationToken(
            user.getId().toString(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user.getId();
    }

    // ---- tests -------------------------------------------------------------

    @Test
    @DisplayName("getProject() second call is served from Redis cache (no DB re-query)")
    void getProject_cacheable() {
        var userId = createUserAndAuthenticate("cache_user_" + UUID.randomUUID().toString().substring(0, 6));
        var project = projectService.createProject(new CreateProjectRequest("Cache Test", null), userId);
        var projectId = project.id();

        // Clear cache so first call hits DB
        cacheManager.getCache(RedisConfig.CACHE_PROJECTS).evict(projectId);

        // First call — cache miss → DB query
        ProjectResponse first = projectService.getProject(projectId, userId);

        // Second call — cache hit → should return same data without DB
        ProjectResponse second = projectService.getProject(projectId, userId);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(first.name()).isEqualTo(second.name());

        // Verify entry exists in Redis cache
        var cached = cacheManager.getCache(RedisConfig.CACHE_PROJECTS).get(projectId);
        assertThat(cached).isNotNull();
    }

    @Test
    @DisplayName("updateProject() evicts cache → next getProject() hits DB")
    void updateProject_evictsCache() {
        var userId = createUserAndAuthenticate("evict_user_" + UUID.randomUUID().toString().substring(0, 6));
        var project = projectService.createProject(new CreateProjectRequest("Before Update", null), userId);
        var projectId = project.id();

        // Warm up cache
        projectService.getProject(projectId, userId);
        assertThat(cacheManager.getCache(RedisConfig.CACHE_PROJECTS).get(projectId)).isNotNull();

        // Update → should evict cache
        projectService.updateProject(projectId, new UpdateProjectRequest("After Update", null, null), userId);

        // Cache entry should be gone
        assertThat(cacheManager.getCache(RedisConfig.CACHE_PROJECTS).get(projectId)).isNull();

        // Next call re-fetches from DB with updated name
        var refreshed = projectService.getProject(projectId, userId);
        assertThat(refreshed.name()).isEqualTo("After Update");
    }
}
