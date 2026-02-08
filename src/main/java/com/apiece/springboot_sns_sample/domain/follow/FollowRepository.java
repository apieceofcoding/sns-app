package com.apiece.springboot_sns_sample.domain.follow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFolloweeIdAndDeletedAtIsNull(Long followerId, Long followeeId);

    List<Follow> findByFollowerIdAndDeletedAtIsNull(Long followerId);

    List<Follow> findByFolloweeIdAndDeletedAtIsNull(Long followeeId);

    boolean existsByFollowerIdAndFolloweeIdAndDeletedAtIsNull(Long followerId, Long followeeId);
}
