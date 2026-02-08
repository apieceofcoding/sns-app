package com.apiece.springboot_sns_sample.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.replyCount = p.replyCount + 1 WHERE p.id = :id")
    void incrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.replyCount = p.replyCount - 1 WHERE p.id = :id AND p.replyCount > 0")
    void decrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.repostCount = p.repostCount + 1 WHERE p.id = :id")
    void incrementRepostCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.repostCount = p.repostCount - 1 WHERE p.id = :id AND p.repostCount > 0")
    void decrementRepostCount(@Param("id") Long id);

    List<Post> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    List<Post> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    List<Post> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentId);

    List<Post> findByUserIdAndParentIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Post> findByUserIdAndParentIdIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndQuoteIdAndDeletedAtIsNull(Long userId, Long quoteId);

    boolean existsByUserIdAndRepostIdAndDeletedAtIsNull(Long userId, Long repostId);

    Optional<Post> findByUserIdAndRepostIdAndDeletedAtIsNull(Long userId, Long repostId);

    List<Post> findByUserIdAndRepostIdInAndDeletedAtIsNull(Long userId, List<Long> repostIds);
}
