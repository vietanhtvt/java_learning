package com.taskflow.kafka;

import com.taskflow.config.KafkaConfig;
import com.taskflow.entity.Notification;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.entity.enums.NotificationType;
import com.taskflow.kafka.event.CommentAddedEvent;
import com.taskflow.kafka.event.TaskAssignedEvent;
import com.taskflow.kafka.event.TaskCompletedEvent;
import com.taskflow.repository.NotificationRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes task events from Kafka and persists Notification entities.
 * Runs on Virtual Threads (spring.threads.virtual.enabled=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @KafkaListener(topics = KafkaConfig.TOPIC_TASK_ASSIGNED,
                   groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onTaskAssigned(@Payload TaskAssignedEvent event) {
        log.info("Received TaskAssignedEvent taskId={} assigneeId={}", event.taskId(), event.assigneeId());

        userRepository.findById(event.assigneeId()).ifPresent(assignee -> {
            Task task = taskRepository.findById(event.taskId()).orElse(null);
            Notification notification = Notification.builder()
                .title("You have been assigned a task")
                .message(String.format("Task \"%s\" has been assigned to you.", event.taskTitle()))
                .type(NotificationType.TASK_ASSIGNED)
                .user(assignee)
                .task(task)
                .build();
            notificationRepository.save(notification);
        });
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_TASK_COMPLETED,
                   groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onTaskCompleted(@Payload TaskCompletedEvent event) {
        log.info("Received TaskCompletedEvent taskId={}", event.taskId());

        // Notify project members — simplified: notify task reporter
        taskRepository.findById(event.taskId()).ifPresent(task -> {
            User reporter = task.getReporter();
            if (reporter != null && !reporter.getId().equals(event.completedById())) {
                Notification notification = Notification.builder()
                    .title("Task completed")
                    .message(String.format("Task \"%s\" has been marked as done.", event.taskTitle()))
                    .type(NotificationType.TASK_COMPLETED)
                    .user(reporter)
                    .task(task)
                    .build();
                notificationRepository.save(notification);
            }
        });
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_COMMENT_ADDED,
                   groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onCommentAdded(@Payload CommentAddedEvent event) {
        log.info("Received CommentAddedEvent taskId={} authorId={}", event.taskId(), event.authorId());

        // Notify task assignee (if different from commenter)
        taskRepository.findById(event.taskId()).ifPresent(task -> {
            User assignee = task.getAssignee();
            if (assignee != null && !assignee.getId().equals(event.authorId())) {
                Notification notification = Notification.builder()
                    .title("New comment on your task")
                    .message(String.format("%s commented on \"%s\": %s",
                        event.authorName(), event.taskTitle(), event.contentPreview()))
                    .type(NotificationType.COMMENT_ADDED)
                    .user(assignee)
                    .task(task)
                    .build();
                notificationRepository.save(notification);
            }
        });
    }
}
