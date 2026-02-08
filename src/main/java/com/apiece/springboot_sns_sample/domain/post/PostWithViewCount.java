package com.apiece.springboot_sns_sample.domain.post;

public record PostWithViewCount(
        Post post,
        Long viewCount
) {
}
