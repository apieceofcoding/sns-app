package com.apiece.springboot_sns_sample.config.auth;

public record AuthLoginResponse(
        String sessionId,
        String username
) {
}
