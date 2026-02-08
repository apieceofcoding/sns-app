package com.apiece.springboot_sns_sample.api.auth;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record AuthSessionInfo(
        String sessionId,
        Object principal,
        LocalDateTime lastRequest,
        boolean expired
) {
    public static AuthSessionInfo from(String sessionId, Object principal, java.util.Date lastRequest, boolean expired) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastRequest.getTime()), ZoneId.systemDefault());
        return new AuthSessionInfo(sessionId, principal, localDateTime, expired);
    }
}
