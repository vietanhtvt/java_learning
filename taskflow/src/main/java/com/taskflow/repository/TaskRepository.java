package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.entity.enums.Priority;
import com.taskflow.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        WHERE t.project.id = :projectId AND t.status = :status
        ORDER BY t.priority DESC, t.dueDate ASC
        """)
    List<Task> findByProjectAndStatus(@Param("projectId") UUID projectId,
                                      @Param("status") TaskStatus status);

    @Query("""
        SELECT t FROM Task t
        WHERE t.assignee.id = :assigneeId AND t.status NOT IN ('DONE', 'CANCELLED')
        ORDER BY t.dueDate ASC NULLS LAST
        """)
    List<Task> findActiveTasksByAssignee(@Param("assigneeId") UUID assigneeId);

    @Query("""
        SELECT t FROM Task t
        WHERE t.dueDate < :today AND t.status NOT IN ('DONE', 'CANCELLED')
        ORDER BY t.dueDate ASC
        """)
    List<Task> findOverdueTasks(@Param("today") LocalDate today);

    @Query("""
        SELECT t FROM Task t
        WHERE t.project.id = :projectId
          AND t.dueDate < :today
          AND t.status NOT IN ('DONE', 'CANCELLED')
        """)
    List<Task> findOverdueTasksByProject(@Param("projectId") UUID projectId,
                                         @Param("today") LocalDate today);

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.reporter
        WHERE t.id = :id
        """)
    Optional<Task> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT t FROM Task t
        WHERE t.project.id = :projectId
          AND (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        """)
    Page<Task> findByProjectWithFilters(@Param("projectId") UUID projectId,
                                        @Param("status") TaskStatus status,
                                        @Param("priority") Priority priority,
                                        @Param("assigneeId") UUID assigneeId,
                                        Pageable pageable);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);
}
