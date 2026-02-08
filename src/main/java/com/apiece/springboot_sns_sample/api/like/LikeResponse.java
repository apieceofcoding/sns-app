package com.apiece.springboot_sns_sample.api.like;

import com.apiece.springboot_sns_sample.domain.like.Like;

import java.time.LocalDateTime;

public record LikeResponse(
        Long id,
        Long userId,
        String username,
        Long postId,
        String postContent,
        LocalDateTime createdAt
) {
    public static LikeResponse from(Like like) {
        return new LikeResponse(
                like.getId(),
                like.getUser().getId(),
                like.getUser().getUsername(),
                like.getPost().getId(),
                like.getPost().getContent(),
                like.getCreatedAt()
        );
    }
}
