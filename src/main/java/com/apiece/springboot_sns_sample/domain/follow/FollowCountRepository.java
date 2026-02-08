package com.apiece.springboot_sns_sample.domain.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FollowCountRepository extends JpaRepository<FollowCount, Long> {

    Optional<FollowCount> findByUserIdAndDeletedAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE FollowCount f SET f.followersCount = f.followersCount + 1 WHERE f.userId = :userId AND f.deletedAt IS NULL")
    int incrementFollowersCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE FollowCount f SET f.followersCount = f.followersCount - 1 WHERE f.userId = :userId AND f.followersCount > 0 AND f.deletedAt IS NULL")
    int decrementFollowersCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE FollowCount f SET f.followeesCount = f.followeesCount + 1 WHERE f.userId = :userId AND f.deletedAt IS NULL")
    int incrementFolloweesCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE FollowCount f SET f.followeesCount = f.followeesCount - 1 WHERE f.userId = :userId AND f.followeesCount > 0 AND f.deletedAt IS NULL")
    int decrementFolloweesCount(@Param("userId") Long userId);
}
