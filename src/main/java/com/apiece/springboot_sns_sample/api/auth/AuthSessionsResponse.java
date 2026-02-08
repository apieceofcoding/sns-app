package com.apiece.springboot_sns_sample.api.auth;

import java.util.List;

public record AuthSessionsResponse(
        int totalSessions,
        List<AuthSessionInfo> sessions
) {
}
