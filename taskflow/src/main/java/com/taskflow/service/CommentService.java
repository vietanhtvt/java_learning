package com.taskflow.service;

import com.taskflow.dto.request.CreateCommentRequest;
import com.taskflow.dto.response.CommentResponse;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.entity.Comment;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.exception.AccessDeniedException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.kafka.NotificationProducer;
import com.taskflow.kafka.event.CommentAddedEvent;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationProducer notificationProducer;

    public PageResponse<CommentResponse> getComments(UUID taskId, UUID userId, Pageable pageable) {
        Task task = findTaskOrThrow(taskId);
        assertMember(task.getProject().getId(), userId);
        return PageResponse.from(
            commentRepository.findByTaskIdWithAuthor(taskId, pageable).map(CommentResponse::from));
    }

    @Transactional
    public CommentResponse addComment(UUID taskId, CreateCommentRequest request, UUID authorId) {
        Task task = findTaskOrThrow(taskId);
        assertMember(task.getProject().getId(), authorId);

        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", authorId));

        Comment comment = Comment.builder()
            .content(request.content())
            .task(task)
            .author(author)
            .build();

        Comment saved = commentRepository.save(comment);

        notificationProducer.sendCommentAdded(
            CommentAddedEvent.of(saved.getId(), task.getId(), task.getTitle(),
                task.getProject().getId(), author.getId(),
                author.getFullName() != null ? author.getFullName() : author.getUsername(),
                request.content()));

        return CommentResponse.from(saved);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean isOwner = projectRepository.existsByIdAndOwnerId(
            comment.getTask().getProject().getId(), userId);

        if (!isAuthor && !isOwner) {
            throw new AccessDeniedException("Only the comment author or project owner can delete comments");
        }

        commentRepository.delete(comment);
    }

    private Task findTaskOrThrow(UUID id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private void assertMember(UUID projectId, UUID userId) {
        if (!projectRepository.isMember(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }
}
