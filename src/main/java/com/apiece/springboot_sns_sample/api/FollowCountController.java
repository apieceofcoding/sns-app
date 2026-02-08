package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.follow.FollowCountResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.follow.FollowCount;
import com.apiece.springboot_sns_sample.domain.follow.FollowCountService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FollowCountController {

    private final FollowCountService followCountService;

    @GetMapping("/api/v1/follow_counts")
    public FollowCountResponse getFollowCount(@AuthUser User user) {
        FollowCount followCount = followCountService.getFollowCount(user.getId());
        return new FollowCountResponse(followCount.getFollowersCount(), followCount.getFolloweesCount());
    }

    @GetMapping("/api/v1/users/{userId}/follow_counts")
    public FollowCountResponse getUserFollowCount(@PathVariable Long userId) {
        FollowCount followCount = followCountService.getFollowCountOrDefault(userId);
        return new FollowCountResponse(followCount.getFollowersCount(), followCount.getFolloweesCount());
    }
}
