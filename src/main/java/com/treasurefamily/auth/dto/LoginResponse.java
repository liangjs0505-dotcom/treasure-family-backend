package com.treasurefamily.auth.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, String username) {
}
