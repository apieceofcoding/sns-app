package com.apiece.springboot_sns_sample.api.follow;

import com.apiece.springboot_sns_sample.domain.follow.Follow;
import com.apiece.springboot_sns_sample.domain.user.User;
import com.apiece.springboot_sns_sample.domain.user.UserService;

import java.time.LocalDateTime;

public record FollowResponse(
        Long id,
        Long followerId,
        String followerUsername,
        Long followeeId,
        String followeeUsername,
        LocalDateTime createdAt
) {
    public static FollowResponse from(Follow follow, UserService userService) {
        User follower = userService.getById(follow.getFollowerId());
        User followee = userService.getById(follow.getFolloweeId());

        return new FollowResponse(
                follow.getId(),
                follow.getFollowerId(),
                follower.getUsername(),
                follow.getFolloweeId(),
                followee.getUsername(),
                follow.getCreatedAt()
        );
    }
}
