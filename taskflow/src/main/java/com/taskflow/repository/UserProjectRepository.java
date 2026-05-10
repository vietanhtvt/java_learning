package com.taskflow.repository;

import com.taskflow.entity.UserProject;
import com.taskflow.entity.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    Optional<UserProject> findByUserIdAndProjectId(UUID userId, UUID projectId);

    List<UserProject> findByProjectId(UUID projectId);

    void deleteByUserIdAndProjectId(UUID userId, UUID projectId);

    @Query("""
        SELECT up.role FROM UserProject up
        WHERE up.user.id = :userId AND up.project.id = :projectId
        """)
    Optional<ProjectRole> findRoleByUserIdAndProjectId(@Param("userId") UUID userId,
                                                        @Param("projectId") UUID projectId);
}
