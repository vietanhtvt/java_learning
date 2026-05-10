package com.taskflow.config;

import com.taskflow.kafka.event.TaskAssignedEvent;
import com.taskflow.kafka.event.TaskCompletedEvent;
import com.taskflow.kafka.event.CommentAddedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaConfig {

    // Topic name constants
    public static final String TOPIC_TASK_ASSIGNED  = "task-assigned";
    public static final String TOPIC_TASK_COMPLETED = "task-completed";
    public static final String TOPIC_COMMENT_ADDED  = "comment-added";

    @Bean
    public NewTopic taskAssignedTopic() {
        return TopicBuilder.name(TOPIC_TASK_ASSIGNED)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic taskCompletedTopic() {
        return TopicBuilder.name(TOPIC_TASK_COMPLETED)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic commentAddedTopic() {
        return TopicBuilder.name(TOPIC_COMMENT_ADDED)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public RecordMessageConverter messageConverter() {
        return new JsonMessageConverter();
    }
}
