package com.apiece.springboot_sns_sample.domain.follow;

import com.apiece.springboot_sns_sample.domain.user.User;
import com.apiece.springboot_sns_sample.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserService userService;
    private final FollowCountService followCountService;

    @Transactional
    public Follow follow(User follower, Long followeeId) {
        User followee = userService.getById(followeeId);

        if (follower.getId().equals(followeeId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        if (followRepository.existsByFollowerIdAndFolloweeIdAndDeletedAtIsNull(follower.getId(), followeeId)) {
            throw new IllegalStateException("Already following this user");
        }

        Follow follow = Follow.create(follower.getId(), followeeId);
        Follow savedFollow = followRepository.save(follow);

        followCountService.incrementFolloweesCount(follower);
        followCountService.incrementFollowersCount(followee);

        return savedFollow;
    }

    @Transactional
    public void unfollow(User follower, Long followeeId) {
        User followee = userService.getById(followeeId);

        Follow follow = followRepository.findByFollowerIdAndFolloweeIdAndDeletedAtIsNull(follower.getId(), followeeId)
                .orElseThrow(() -> new IllegalStateException("Not following this user"));

        follow.delete();

        followCountService.decrementFolloweesCount(follower);
        followCountService.decrementFollowersCount(followee);
    }

    public List<Follow> getFollowers(User user) {
        return followRepository.findByFolloweeIdAndDeletedAtIsNull(user.getId());
    }

    public List<Follow> getFollowees(User user) {
        return followRepository.findByFollowerIdAndDeletedAtIsNull(user.getId());
    }

    public boolean isFollowing(User user, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeIdAndDeletedAtIsNull(user.getId(), followeeId);
    }
}
