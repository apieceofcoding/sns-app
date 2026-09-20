package com.apiece.springboot_sns_sample.domain.recommend;

public record SegmentResponse(
        Long userId,
        String segment
) {
}
