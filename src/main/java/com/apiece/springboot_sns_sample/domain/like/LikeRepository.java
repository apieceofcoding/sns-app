package com.apiece.springboot_sns_sample.domain.like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    List<Like> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<Like> findByIdAndDeletedAtIsNull(Long id);

    List<Like> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndPostIdAndDeletedAtIsNull(Long userId, Long postId);

    Optional<Like> findByUserIdAndPostIdAndDeletedAtIsNull(Long userId, Long postId);

    List<Like> findByUserIdAndPostIdInAndDeletedAtIsNull(Long userId, List<Long> postIds);
}
