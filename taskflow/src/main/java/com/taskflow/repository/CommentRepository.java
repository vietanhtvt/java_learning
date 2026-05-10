package com.taskflow.repository;

import com.taskflow.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.author
        WHERE c.task.id = :taskId
        ORDER BY c.createdAt ASC
        """)
    Page<Comment> findByTaskIdWithAuthor(@Param("taskId") UUID taskId, Pageable pageable);

    long countByTaskId(UUID taskId);
}
