package com.apiece.springboot_sns_sample.domain.post;

public record PostWithUserContext(
        Post post,
        Long viewCount,
        Boolean isLikedByMe,
        Long likeIdByMe,
        Boolean isRepostedByMe,
        Long repostIdByMe,
        // 관련 게시물 정보
        Post repostedPost,
        Post quotedPost,
        Post parentPost,
        // 재게시 정보
        Long repostedByUserId,
        String repostedByUsername
) {
}
