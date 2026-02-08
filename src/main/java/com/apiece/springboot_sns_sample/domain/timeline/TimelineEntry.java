package com.apiece.springboot_sns_sample.domain.timeline;

public record TimelineEntry(
        Long postId,
        Double score
) {
}
