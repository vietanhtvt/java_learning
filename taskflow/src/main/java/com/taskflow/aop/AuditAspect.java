package com.taskflow.aop;

import com.taskflow.entity.AuditLog;
import com.taskflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Intercepts methods annotated with @Auditable and writes a record to audit_log.
 * Uses REQUIRES_NEW so the audit entry is saved even if the business transaction rolls back.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(com.taskflow.aop.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        Object result = joinPoint.proceed(); // Run the actual method first

        try {
            saveAuditLog(auditable, joinPoint.getArgs());
        } catch (Exception ex) {
            // Audit failure must NOT affect business flow
            log.error("Failed to write audit log for action={}", auditable.action(), ex);
        }

        return result;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveAuditLog(Auditable auditable, Object[] args) {
        String actor = resolveActor();
        String entityId = args.length > 0 ? String.valueOf(args[0]) : null;
        String entityType = auditable.entityType().isBlank()
            ? resolveEntityType(auditable.action())
            : auditable.entityType();

        AuditLog log = AuditLog.builder()
            .action(auditable.action())
            .entityType(entityType)
            .entityId(entityId)
            .actor(actor)
            .details("args=" + Arrays.toString(args))
            .build();

        auditLogRepository.save(log);
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private String resolveEntityType(String action) {
        // Derive from action name e.g. CREATE_PROJECT → Project
        if (action.contains("PROJECT")) return "Project";
        if (action.contains("TASK"))    return "Task";
        if (action.contains("COMMENT")) return "Comment";
        return "Unknown";
    }
}
