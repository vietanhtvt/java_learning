package com.taskflow.kafka;

import com.taskflow.config.KafkaConfig;
import com.taskflow.kafka.event.CommentAddedEvent;
import com.taskflow.kafka.event.TaskAssignedEvent;
import com.taskflow.kafka.event.TaskCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTaskAssigned(TaskAssignedEvent event) {
        send(KafkaConfig.TOPIC_TASK_ASSIGNED, event.taskId().toString(), event);
    }

    public void sendTaskCompleted(TaskCompletedEvent event) {
        send(KafkaConfig.TOPIC_TASK_COMPLETED, event.taskId().toString(), event);
    }

    public void sendCommentAdded(CommentAddedEvent event) {
        send(KafkaConfig.TOPIC_COMMENT_ADDED, event.taskId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic={} key={}: {}", topic, key, ex.getMessage());
            } else {
                log.debug("Sent event to topic={} partition={} offset={}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
