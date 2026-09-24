package com.ems.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String username,
        String role
) {
}
