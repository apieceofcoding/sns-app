package com.apiece.springboot_sns_sample.domain.recommendation;

public record SegmentResponse(
        Long userId,
        String segment
) {
}
