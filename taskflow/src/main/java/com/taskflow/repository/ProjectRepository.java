package com.taskflow.repository;

import com.taskflow.entity.Project;
import com.taskflow.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwnerIdAndStatus(UUID ownerId, ProjectStatus status);

    @Query("""
        SELECT p FROM Project p
        JOIN p.members up
        WHERE up.user.id = :userId
        ORDER BY p.updatedAt DESC
        """)
    Page<Project> findProjectsByMemberId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Project p JOIN FETCH p.owner WHERE p.id = :id")
    Optional<Project> findByIdWithOwner(@Param("id") UUID id);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("""
        SELECT CASE WHEN COUNT(up) > 0 THEN TRUE ELSE FALSE END
        FROM UserProject up
        WHERE up.project.id = :projectId AND up.user.id = :userId
        """)
    boolean isMember(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
