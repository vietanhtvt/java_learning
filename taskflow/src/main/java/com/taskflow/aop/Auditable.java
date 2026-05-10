package com.taskflow.aop;

import java.lang.annotation.*;

/**
 * Marks a service method to be recorded in the audit_log table.
 * Captured by AuditAspect via @Around.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
    String entityType() default "";
}
