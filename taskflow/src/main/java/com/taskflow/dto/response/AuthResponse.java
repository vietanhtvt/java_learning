package com.taskflow.dto.response;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    UUID userId,
    String username,
    String email
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                   UUID userId, String username, String email) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, username, email);
    }
}
