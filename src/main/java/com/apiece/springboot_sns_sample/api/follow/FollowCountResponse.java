package com.apiece.springboot_sns_sample.api.follow;

public record FollowCountResponse(
        long followersCount,
        long followeesCount
) {
}
