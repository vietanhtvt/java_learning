package com.taskflow.config;

import com.taskflow.entity.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final TaskRepository taskRepository;

    /**
     * Counter: incremented each time a task is completed.
     * Exposed at: /actuator/metrics/taskflow.tasks.completed
     */
    @Bean
    public Counter taskCompletedCounter(MeterRegistry registry) {
        return Counter.builder("taskflow.tasks.completed")
            .description("Total number of tasks marked as DONE")
            .tag("app", "taskflow")
            .register(registry);
    }

    /**
     * Counter: incremented each time a task is created.
     */
    @Bean
    public Counter taskCreatedCounter(MeterRegistry registry) {
        return Counter.builder("taskflow.tasks.created")
            .description("Total number of tasks created")
            .tag("app", "taskflow")
            .register(registry);
    }

    /**
     * Gauge: live count of tasks currently IN_PROGRESS across all projects.
     * Value is re-evaluated on every scrape (lazy DB query).
     * Exposed at: /actuator/metrics/taskflow.tasks.in_progress
     */
    @Bean
    public Gauge tasksInProgressGauge(MeterRegistry registry) {
        return Gauge.builder("taskflow.tasks.in_progress",
                taskRepository,
                repo -> repo.countByStatus(TaskStatus.IN_PROGRESS))
            .description("Current number of tasks in progress (all projects)")
            .tag("app", "taskflow")
            .register(registry);
    }
}
