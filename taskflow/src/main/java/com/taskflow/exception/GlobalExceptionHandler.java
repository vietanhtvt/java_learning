package com.taskflow.exception;

import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://taskflow.api/errors/not-found"));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://taskflow.api/errors/forbidden"));
        return problem;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ProblemDetail handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://taskflow.api/errors/forbidden"));
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setType(URI.create("https://taskflow.api/errors/business"));
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("https://taskflow.api/errors/unauthorized"));
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://taskflow.api/errors/validation"));
        problem.setProperty("errors", errors);
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneral(Exception ex) {
        logger.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://taskflow.api/errors/internal"));
        return problem;
    }
}
