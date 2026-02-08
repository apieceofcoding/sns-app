package com.apiece.springboot_sns_sample.domain.follow;

import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowCountService {

    private final FollowCountRepository followCountRepository;

    public FollowCount getFollowCount(Long userId) {
        return followCountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new IllegalStateException("FollowCount not found for user"));
    }

    public FollowCount getFollowCountOrDefault(Long userId) {
        return followCountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElse(FollowCount.createDefault(userId));
    }

    @Transactional
    @Retryable(DataIntegrityViolationException.class)
    public void incrementFollowersCount(User user) {
        int updated = followCountRepository.incrementFollowersCount(user.getId());
        if (updated == 0) {
            createFollowCount(user.getId());
            followCountRepository.incrementFollowersCount(user.getId());
        }
    }

    @Transactional
    public void decrementFollowersCount(User user) {
        followCountRepository.decrementFollowersCount(user.getId());
    }

    @Transactional
    @Retryable(DataIntegrityViolationException.class)
    public void incrementFolloweesCount(User user) {
        int updated = followCountRepository.incrementFolloweesCount(user.getId());
        if (updated == 0) {
            createFollowCount(user.getId());
            followCountRepository.incrementFolloweesCount(user.getId());
        }
    }

    @Transactional
    public void decrementFolloweesCount(User user) {
        followCountRepository.decrementFolloweesCount(user.getId());
    }

    private void createFollowCount(Long userId) {
        FollowCount followCount = FollowCount.create(userId);
        followCountRepository.save(followCount); // userId 중복일 경우 DataIntegrityViolationException
    }
}
