package com.taskflow.dto.response;

import com.taskflow.entity.User;

import java.util.UUID;

public record UserSummaryResponse(
    UUID id,
    String username,
    String fullName,
    String email
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail()
        );
    }
}
