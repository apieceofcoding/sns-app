package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.auth.AuthSessionInfo;
import com.apiece.springboot_sns_sample.api.auth.AuthSessionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SessionRegistry sessionRegistry;

    @GetMapping("/api/v1/sessions")
    public AuthSessionsResponse getAllSessions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthSessionsResponse(0, List.of());
        }

        List<AuthSessionInfo> sessions = sessionRegistry.getAllSessions(authentication.getPrincipal(), false).stream()
                .map(sessionInfo -> AuthSessionInfo.from(
                        sessionInfo.getSessionId(),
                        sessionInfo.getPrincipal(),
                        sessionInfo.getLastRequest(),
                        sessionInfo.isExpired()
                ))
                .collect(Collectors.toList());

        return new AuthSessionsResponse(sessions.size(), sessions);
    }
}
