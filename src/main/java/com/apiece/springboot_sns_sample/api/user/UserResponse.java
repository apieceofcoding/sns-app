package com.apiece.springboot_sns_sample.api.user;

import com.apiece.springboot_sns_sample.domain.user.User;

public record UserResponse(
        Long id,
        String username,
        Long profileMediaId,
        String profileImageUrl
) {
    public static UserResponse from(User user, String profileImageUrl) {
        return new UserResponse(user.getId(), user.getUsername(), user.getProfileMediaId(), profileImageUrl);
    }
}
