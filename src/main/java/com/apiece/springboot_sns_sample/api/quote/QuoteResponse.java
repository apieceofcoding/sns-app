package com.apiece.springboot_sns_sample.api.quote;

import com.apiece.springboot_sns_sample.domain.post.Post;

import java.time.LocalDateTime;

public record QuoteResponse(
        Long id,
        String content,
        Long userId,
        String username,
        Long userProfileMediaId,
        Long quoteId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static QuoteResponse from(Post post) {
        return new QuoteResponse(
                post.getId(),
                post.getContent(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getProfileMediaId(),
                post.getQuoteId(),
                post.getCreatedAt(),
                post.getModifiedAt()
        );
    }
}
