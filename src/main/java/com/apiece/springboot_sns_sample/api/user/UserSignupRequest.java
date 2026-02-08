package com.apiece.springboot_sns_sample.api.user;

public record UserSignupRequest(
        String username,
        String password
) {
}
