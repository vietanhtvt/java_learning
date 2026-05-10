package com.taskflow.controller;

import com.taskflow.dto.response.NotificationResponse;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get paginated notifications for the current user")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(
            notificationService.getNotifications(extractUserId(user), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @AuthenticationPrincipal UserDetails user) {

        long count = notificationService.getUnreadCount(extractUserId(user));
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable UUID notificationId,
        @AuthenticationPrincipal UserDetails user) {

        notificationService.markAsRead(notificationId, extractUserId(user));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllAsRead(extractUserId(user));
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
